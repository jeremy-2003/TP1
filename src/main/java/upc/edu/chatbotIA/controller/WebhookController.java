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

import upc.edu.chatbotIA.model.BlockedUser;
import upc.edu.chatbotIA.model.Relation;
import upc.edu.chatbotIA.model.RelationAdviserCustomer;
import upc.edu.chatbotIA.repository.RelationRepository;
import upc.edu.chatbotIA.service.*;
import upc.edu.chatbotIA.util.AudioDownloader;

@RestController("/webhook")
public class WebhookController {
    private final RelationAdviserCustomerService relationAdviserCustomerService;
    private final ObjectMapper objectMapper;
    private final AudioDownloader audioDownloader;
    private final TranscriptionService transcriptionService;
    private final ChatGptService chatGptService;
    private final WhatsAppService whatsAppService;
    private final SheetsService sheetsService;

    private final RelationRepository relationRepository;
    private final BlockedUserService blockedUserService;

    @Autowired
    public WebhookController(ObjectMapper objectMapper, AudioDownloader audioDownloader,
                             TranscriptionService transcriptionService, ChatGptService chatGptService, WhatsAppService whatsAppService,
                             SheetsService sheetsService, RelationRepository relationRepository, BlockedUserService blockedUserService,
                             RelationAdviserCustomerService relationAdviserCustomerService) {
        this.objectMapper = objectMapper;
        this.audioDownloader = audioDownloader;
        this.transcriptionService = transcriptionService;
        this.chatGptService = chatGptService;
        this.whatsAppService = whatsAppService;
        this.sheetsService = sheetsService;
        this.relationRepository = relationRepository;
        this.blockedUserService = blockedUserService;
        this.relationAdviserCustomerService = relationAdviserCustomerService;
    }
    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private Map<String, Boolean> isFirstMessage = new HashMap<>();
    private Map<String, Boolean> isWaitingForAdvisor = new HashMap<>();

    @PostMapping("/incoming-whatsapp")
    public ResponseEntity<Void> receiveWhatsApp(@RequestBody String requestBody) {
        System.out.println("Ingreso a capturar el mensaje");
        try {
            WhatsAppWebhookInboundMessageResult messages = objectMapper.readValue(requestBody,
                    WhatsAppWebhookInboundMessageResult.class);
            for (WhatsAppWebhookInboundMessageData messageData : messages.getResults()) {
                String senderId = messageData.getFrom();
                Optional<Relation> existingRelation = relationRepository.findByUserNumber(senderId);
                if (blockedUserService.isUserBlocked(senderId)) {
                    whatsAppService.sendTextMessage(senderId, "Estás temporalmente bloqueado. Por favor, espera unos minutos antes de intentar enviar mensajes nuevamente.");
                    return ResponseEntity.ok().build();
                }
                BlockedUser blockedUser = blockedUserService.findByUserId(senderId);
                if (blockedUser != null && blockedUser.getBlockTime().isBefore(LocalDateTime.now())) {
                    // El usuario ha sido desbloqueado, establece isFirstMessage a true
                    blockedUserService.unblockUser(blockedUser.getUserNumber()); // Elimina el registro del usuario bloqueado
                    isFirstMessage.remove(senderId);
                }
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
                                    isFirstMessage.put(senderId, false);
                                    saveRelation(senderId, text, "", false); // Guardar la relación con el DNI proporcionado
                                } else if (userData.equals("El DNI ingresado no es válido. Por favor, ingresa un número de DNI de 8 dígitos.")) {
                                    whatsAppService.sendTextMessage(senderId, userData);
                                    // Mantener el estado de primer mensaje para volver a solicitar el DNI
                                    isFirstMessage.put(senderId, true);
                                }
                                else {
                                    whatsAppService.sendTextMessage(senderId, userData);
                                    isFirstMessage.put(senderId, false);
                                }
                            } else {
                                Optional<Relation> relationToUpdate = relationRepository.findByUserNumber(senderId);
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
                if (userData.equals("El DNI ingresado no es válido. Por favor, ingresa un número de DNI de 8 dígitos.")) {
                    whatsAppService.sendTextMessage(senderId, userData);
                    // Mantener el estado de primer mensaje para volver a solicitar el DNI
                    isFirstMessage.put(senderId, true);
                } else {
                    whatsAppService.sendTextMessage(senderId, userData);
                    isFirstMessage.put(senderId, false);
                }
            } else {
                // Verificar si el usuario tiene una relación activa con un asesor
                RelationAdviserCustomer relacionAsesorCliente = relationAdviserCustomerService.encontrarConversacionesActivas(senderId, true);
                System.out.println("Se encontro: " +  relacionAsesorCliente + "semnderid: " +  senderId);
                if (relacionAsesorCliente != null) {
                    // Reenviar el mensaje al asesor
                    whatsAppService.sendTextMessage(relacionAsesorCliente.getAdviserNumber(), "Mensaje del usuario " + senderId + ": " + text);
                } else {
                    String textasesor = textMessage.getText().toUpperCase();
                    if (textasesor.equals("ASESOR")) {
                        Optional<String> asesorDisponible = relationAdviserCustomerService.buscarAsesorDisponible();
                        System.out.println("Asesor disponible: " + asesorDisponible);
                        if (asesorDisponible.isPresent()) {
                            String asesorNumber = asesorDisponible.get();
                            whatsAppService.sendTextMessage(asesorNumber, "Atención: El usuario " + senderId + " ha solicitado un asesor.");
                            System.out.println("Se asigno al usuario " + senderId + " el asesor  con numero " + asesorNumber);
                            guardarRelacionAsesorCliente(senderId, asesorNumber);

                            // Desactivar el procesamiento de mensajes para este usuario y marcar que está esperando al asesor
                            isFirstMessage.put(senderId, false);
                            isWaitingForAdvisor.put(senderId, true);
                        } else {
                            whatsAppService.sendTextMessage(senderId, "Lo sentimos, no hay asesores disponibles en este momento. Por favor, intente más tarde.");
                        }
                    } else {
                        ChatMessage chatMessage = chatGptService.getChatCompletion(senderId, text);
                        String responseText = chatMessage.getContent();
                        whatsAppService.sendTextMessage(senderId, responseText);
                    }
                }
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
            if (!dni.matches("\\d{8}")) {
                int attempts = failedAttempts.getOrDefault(senderId, 0) + 1;
                failedAttempts.put(senderId, attempts);

                // Verificar si se supera el límite de intentos fallidos
                if (attempts >= 3) {
                    blockedUserService.blockUser(senderId);
                    failedAttempts.remove(senderId);
                    return "Has sido bloqueado temporalmente debido a múltiples intentos fallidos. Por favor, intenta nuevamente más tarde.";
                }
                return "El DNI ingresado no es válido. Por favor, ingresa un número de DNI de 8 dígitos.";
            }

            List<List<Object>> excelData = sheetsService.connectToGoogleSheets();
            for (List<Object> row : excelData) {
                if (row.size() > 2 && row.get(2) instanceof String && row.get(2).equals(dni)) {
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
        relation.setUserNumber(senderId);
        relation.setDni(Integer.parseInt(dni));
        relation.setName(name);
        relation.setExpirationTime(LocalDateTime.now().plusMinutes(5));
        relation.setActive(true);
        relation.setLastInteractionTime(LocalDateTime.now());
        relation.setClient(client);
        relationRepository.save(relation);
    }

    private void guardarRelacionAsesorCliente(String customerId, String adviserNumber) {
        RelationAdviserCustomer relacionAsesorCustomer = new RelationAdviserCustomer();
        relacionAsesorCustomer.setAdviserNumber(adviserNumber);
        relacionAsesorCustomer.setUserNumber(customerId);
        relacionAsesorCustomer.setFirstTimeInteraction(LocalDateTime.now());
        relacionAsesorCustomer.setLastTimeInteraction(LocalDateTime.now());
        relacionAsesorCustomer.setActive(true);
        relationAdviserCustomerService.guardarRelacion(relacionAsesorCustomer);
    }
}
