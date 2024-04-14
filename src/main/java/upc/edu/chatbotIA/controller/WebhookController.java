package upc.edu.chatbotIA.controller;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobip.model.WhatsAppWebhookInboundMessage;
import com.infobip.model.WhatsAppWebhookInboundMessageData;
import com.infobip.model.WhatsAppWebhookInboundMessageResult;
import com.infobip.model.WhatsAppWebhookInboundTextMessage;
import com.infobip.model.WhatsAppWebhookInboundVoiceMessage;

import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import upc.edu.chatbotIA.model.Relation;
import upc.edu.chatbotIA.repository.RelationRepository;
import upc.edu.chatbotIA.service.ChatGptService;
import upc.edu.chatbotIA.service.SheetsService;
import upc.edu.chatbotIA.service.TranscriptionService;
import upc.edu.chatbotIA.service.WhatsAppService;
import upc.edu.chatbotIA.util.AudioDownloader;

@RestController
public class WebhookController {

    private final ObjectMapper objectMapper;
    private final AudioDownloader audioDownloader;
    private final TranscriptionService transcriptionService;
    private final ChatGptService chatGptService;
    private final WhatsAppService whatsAppService;
    private final SheetsService sheetsService;

    private final RelationRepository relationRepository;

    @Autowired
    public WebhookController(ObjectMapper objectMapper, AudioDownloader audioDownloader,
                             TranscriptionService transcriptionService, ChatGptService chatGptService, WhatsAppService whatsAppService,
                             SheetsService sheetsService, RelationRepository relationRepository) {
        this.objectMapper = objectMapper;
        this.audioDownloader = audioDownloader;
        this.transcriptionService = transcriptionService;
        this.chatGptService = chatGptService;
        this.whatsAppService = whatsAppService;
        this.sheetsService = sheetsService;
        this.relationRepository = relationRepository;

    }

    private Map<String, Boolean> isFirstMessage = new HashMap<>();

    @PostMapping("/incoming-whatsapp")
    public ResponseEntity<Void> receiveWhatsApp(@RequestBody String requestBody) {
        try {
            WhatsAppWebhookInboundMessageResult messages = objectMapper.readValue(requestBody,
                    WhatsAppWebhookInboundMessageResult.class);
            for (WhatsAppWebhookInboundMessageData messageData : messages.getResults()) {
                String senderId = messageData.getFrom();
                Optional<Relation> existingRelation = relationRepository.findByUserId(senderId);
                if (existingRelation.isPresent() && !Objects.equals(existingRelation.get().getName(), "")) {
                    Relation relation = existingRelation.get();
                    LocalDateTime now = LocalDateTime.now();
                    if (relation.getActive() && relation.getExpirationTime().isAfter(now)) {
                        // Si el usuario ya está registrado, la relación está activa y no ha expirado, procesar el mensaje normalmente
                        System.out.println("Entró en este flujo 3ero");
                        isFirstMessage.remove(senderId);
                        processMessage(senderId, messageData.getMessage());
                        // Actualizar la marca de tiempo de interacción
                        relation.setLastInteractionTime(now);
                        LocalDateTime expirationThreshold = relation.getExpirationTime().minusDays(5); // Umbral de extensión de 5 días
                        if (relation.getLastInteractionTime().isAfter(expirationThreshold)) {
                            relation.setExpirationTime(now.plusDays(30)); // Extender la fecha de expiración por 30 días más
                        }
                        relationRepository.save(relation);
                    } else if (!relation.getActive() && relation.getExpirationTime().isAfter(now)) {
                        // Si la relación está inactiva pero no ha expirado, enviar un saludo personalizado y procesar el mensaje normalmente
                        String welcomeMessage = "Hola " + relation.getName() + ", ¡bienvenido de nuevo! ¿En qué podemos ayudarte?";
                        whatsAppService.sendTextMessage(senderId, welcomeMessage);
                        // Actualizar la marca de tiempo de interacción y reactivar la relación
                        relation.setLastInteractionTime(now);
                        relation.setActive(true);
                        relationRepository.save(relation);
                    } else {
                        // Si la relación ha expirado, eliminar la relación y solicitar el DNI nuevamente
                        relationRepository.delete(relation);
                        if (!isFirstMessage.containsKey(senderId)) {
                            isFirstMessage.put(senderId, true);
                            sendWelcomeMessage(senderId);
                        } else {
                            WhatsAppWebhookInboundMessage message = messageData.getMessage();
                            if (message.getType().equals("TEXT")) {
                                WhatsAppWebhookInboundTextMessage textMessage = (WhatsAppWebhookInboundTextMessage) message;
                                String text = textMessage.getText();
                                String userData = searchDniInExcel(senderId, text);
                                whatsAppService.sendTextMessage(senderId, userData);
                                isFirstMessage.put(senderId, false);
                            }
                        }
                    }
                } else {
                    // Si el usuario no está registrado, verificar si es el primer mensaje
                    if (!isFirstMessage.containsKey(senderId)) {
                        isFirstMessage.put(senderId, true);
                        sendWelcomeMessage(senderId);
                    } else {
                        WhatsAppWebhookInboundMessage message = messageData.getMessage();
                        String messageType = String.valueOf(message.getType());
                        if (messageType.equals("TEXT")) {
                            WhatsAppWebhookInboundTextMessage textMessage = (WhatsAppWebhookInboundTextMessage) message;
                            String text = textMessage.getText();
                            if (isFirstMessage.get(senderId)) {
                                String userData = searchDniInExcel(senderId, text);
                                if (userData.equals("Vemos que no eres cliente nuestro. Podrias por favor brindarnos tu nombre para continuar con tu consulta.")) {
                                    whatsAppService.sendTextMessage(senderId, userData);
                                    saveRelation(senderId, text, "", false); // Guardar la relación con el DNI proporcionado
                                } else {
                                    whatsAppService.sendTextMessage(senderId, userData);
                                }
                                isFirstMessage.put(senderId, false);
                            } else {
                                Optional<Relation> relationToUpdate = relationRepository.findByUserId(senderId);
                                if (relationToUpdate.isPresent()) {
                                    Relation relation = relationToUpdate.get();
                                    relation.setName(text);
                                    relationRepository.save(relation);
                                    String welcomeMessage = "Hola " + text + ", gracias por proporcionarnos tu nombre. ¿En qué podemos ayudarte?";
                                    whatsAppService.sendTextMessage(senderId, welcomeMessage);
                                    isFirstMessage.remove(senderId);
                                }
                            }
                        }
                    }
                }
            }
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    private void processMessage(String senderId, WhatsAppWebhookInboundMessage message) throws IOException {
        String messageType = String.valueOf(message.getType());
        if (messageType.equals("TEXT")) {
            WhatsAppWebhookInboundTextMessage textMessage = (WhatsAppWebhookInboundTextMessage) message;
            String text = textMessage.getText();
            if (isFirstMessage.containsKey(senderId) && isFirstMessage.get(senderId)) {
                String userData = searchDniInExcel(senderId, text);
                whatsAppService.sendTextMessage(senderId, userData);
                isFirstMessage.put(senderId, false);
            } else {
                ChatMessage chatMessage = chatGptService.getChatCompletion(senderId, text);
                String responseText = chatMessage.getContent();
                whatsAppService.sendTextMessage(senderId, responseText);
            }
        } else if (messageType.equals("VOICE")) {
            WhatsAppWebhookInboundVoiceMessage voiceMessage = (WhatsAppWebhookInboundVoiceMessage) message;
            String voiceUrl = voiceMessage.getUrl();
            System.out.println("Voice message received:");
            System.out.println("URL: " + voiceUrl);
            // Descargar el audio desde la URL y guardar con extensión MP3
            File mp3File = audioDownloader.downloadAudio(voiceUrl);
            // Transcribir el audio a texto
            String transcription = transcriptionService.transcribeAudio(mp3File);
            ChatMessage chatMessage = chatGptService.getChatCompletion(senderId, transcription);
            String responseText = chatMessage.getContent();
            whatsAppService.sendTextMessage(senderId, responseText);
            System.out.println("Transcription: " + transcription);
            chatGptService.getChatCompletion(senderId, transcription);
        } else {
            System.out.println("Unsupported message type: " + messageType);
        }
    }

    private void sendWelcomeMessage(String senderId) {
        // Envía un mensaje de bienvenida y solicita el DNI
        String welcomeMessage = "¡Bienvenido! Por favor, ingresa tu número de DNI:";
        whatsAppService.sendTextMessage(senderId, welcomeMessage);
    }

    private String searchDniInExcel(String senderId, String dni) {
        try {
            List<List<Object>> excelData = sheetsService.connectToGoogleSheets(); // Obtener datos del Excel
            for (List<Object> row : excelData) {
                if (row.size() > 2 && row.get(2) instanceof String && row.get(2).equals(dni)) {
                    // Si encuentra el DNI, devuelve una cadena de texto con los datos relacionados
                    String name = row.get(0) + " " + row.get(1);
                    saveRelation(senderId, dni, name, true); // Guardar relación en la base de datos
                    String userData = "Hola " + name + ", es un gusto que te comuniques con nosotros. ¿En qué podemos ayudarte?";
                    System.out.println("Se ingreso a reconcer al usuario: " + userData);
                    return userData;
                }
            }
            System.out.println("No se encontró el DNI en el Excel. DNI: " + dni);
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
        return "Vemos que no eres cliente nuestro. Podrias por favor brindarnos tu nombre para continuar con tu consulta.";
    }

    private void saveRelation(String senderId, String dni, String name, Boolean client) {
        Relation relation = new Relation();
        relation.setUserId(senderId);
        relation.setDni(Integer.parseInt(dni));
        relation.setName(name);
        relation.setExpirationTime(LocalDateTime.now().plusMinutes(5));
        relation.setActive(true);
        relation.setLastInteractionTime(LocalDateTime.now());
        relation.setClient(client);
        relationRepository.save(relation);
    }
}
