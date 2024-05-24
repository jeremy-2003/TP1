package upc.edu.chatbotIA.controller;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobip.model.*;

import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import upc.edu.chatbotIA.model.*;
import upc.edu.chatbotIA.repository.AdviserRepository;
import upc.edu.chatbotIA.repository.RelationRepository;
import upc.edu.chatbotIA.service.*;
import upc.edu.chatbotIA.util.AudioDownloader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RestController("/webhook")
public class WebhookController {
    private final RelationAdviserCustomerService relationAdviserCustomerService;
    private final ObjectMapper objectMapper;
    private final AudioDownloader audioDownloader;
    private final TranscriptionService transcriptionService;
    private final ChatGptService chatGptService;
    private final WhatsAppService whatsAppService;
    private final SheetsService sheetsService;
    private final SentimentAnalysisService sentimentAnalysisService;

    private final RelationRepository relationRepository;
    private final BlockedUserService blockedUserService;
    private final AdviserRepository adviserRepository;

    private final SurveyResponseService surveyResponseService;

    private Map<String, Boolean> isSurveyInProgress = new HashMap<>();
    private Map<String, Integer> currentSurveyQuestion = new HashMap<>();

    @Autowired
    public WebhookController(ObjectMapper objectMapper, AudioDownloader audioDownloader,
                             TranscriptionService transcriptionService, ChatGptService chatGptService, WhatsAppService whatsAppService,
                             SheetsService sheetsService, RelationRepository relationRepository, BlockedUserService blockedUserService,
                             RelationAdviserCustomerService relationAdviserCustomerService, SentimentAnalysisService sentimentAnalysisService,
                             SurveyResponseService surveyResponseService, AdviserRepository adviserRepository) {
        this.objectMapper = objectMapper;
        this.audioDownloader = audioDownloader;
        this.transcriptionService = transcriptionService;
        this.chatGptService = chatGptService;
        this.whatsAppService = whatsAppService;
        this.sheetsService = sheetsService;
        this.relationRepository = relationRepository;
        this.blockedUserService = blockedUserService;
        this.relationAdviserCustomerService = relationAdviserCustomerService;
        this.sentimentAnalysisService = sentimentAnalysisService;
        this.surveyResponseService = surveyResponseService;
        this.adviserRepository = adviserRepository;
    }
    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private Map<String, Boolean> isFirstMessage = new HashMap<>();
    private Map<String, Boolean> isWaitingForAdvisor = new HashMap<>();

    @PostMapping("/incoming-whatsapp")
    public ResponseEntity<Void> receiveWhatsApp(@RequestBody String requestBody) {
        try {
            WhatsAppWebhookInboundMessageResult messages = objectMapper.readValue(requestBody,
                    WhatsAppWebhookInboundMessageResult.class);
            for (WhatsAppWebhookInboundMessageData messageData : messages.getResults()) {
                String senderId = messageData.getFrom();
                // Verificar si el remitente es un asesor
                String adviserMessage = null;
                WhatsAppWebhookInboundMessage messageAdviser = messageData.getMessage();
                String messageTypeAdviser = String.valueOf(messageAdviser.getType());
                if (messageTypeAdviser.equals("TEXT")) {
                    WhatsAppWebhookInboundTextMessage textMessage = (WhatsAppWebhookInboundTextMessage) messageData.getMessage();
                    adviserMessage = textMessage.getText();
                }
                List<Adviser> advisers = adviserRepository.findAllAdviserNumbers();
                for (Adviser adviser : advisers) {
                    if (adviser.getAdviserNumber().equals(senderId)) {
                        // Verificar si el asesor tiene una relación activa de atención
                        RelationAdviserCustomer relacionAsesorCliente = relationAdviserCustomerService.encontrarConversacionesActivasAdviserNumber(senderId, true);
                        if (relacionAsesorCliente != null) {
                            if (adviserMessage != null && adviser.getAdviserNumber().equals(senderId) && adviserMessage.equalsIgnoreCase("FINALIZADO")) {
                                String customerNumber = relacionAsesorCliente.getUserNumber();
                                // Finalizar la relación entre el asesor y el cliente
                                relationAdviserCustomerService.finalizarConversacion(relacionAsesorCliente);
                                // Enviar mensaje al cliente indicando que está de vuelta con el chatbot
                                String welcomeBackMessage = "Hola, soy TeleBuddy. Estás de vuelta conmigo. ¿En qué más puedo ayudarte?";
                                whatsAppService.sendTextMessage(customerNumber, welcomeBackMessage);
                                Relation relation = relationRepository.findByUserNumber(customerNumber).orElse(null);
                                if (relation != null) {
                                    LocalDateTime now = LocalDateTime.now();
                                    relation.setLastInteractionTime(LocalDateTime.now());
                                    LocalDateTime expirationThreshold = relation.getExpirationTime().minusDays(5); // Umbral de extensión de 5 días
                                    if (relation.getLastInteractionTime().isAfter(expirationThreshold)) {
                                        relation.setExpirationTime(now.plusDays(30)); // Extender la fecha de expiración por 30 días más
                                    }
                                    relationRepository.save(relation);
                                }
                                whatsAppService.sendTextMessage(senderId, "Has finalizado la conversación con el cliente de número" + customerNumber + " y nombre " + relation.getName());
                                return ResponseEntity.ok().build();
                            } else {
                                // El asesor tiene una relación activa de atención
                                String adviserMessage2 = "Tienes una conversación de atención activa. El único mensaje aceptado es 'FINALIZADO' para dar por culminada la atención.";
                                whatsAppService.sendTextMessage(senderId, adviserMessage2);
                                return ResponseEntity.ok().build();
                            }
                        } else {
                            // El asesor no tiene una relación activa de atención
                            String adviserMessage2 = "Eres un asistente de atención al cliente. Solo puedes utilizar este chat para recibir solicitudes para conectarse al chat con el cliente y cerrar los chats una vez terminada la atención.";
                            whatsAppService.sendTextMessage(senderId, adviserMessage2);
                            return ResponseEntity.ok().build();
                        }
                    }
                }
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
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    private void processMessage(String senderId, WhatsAppWebhookInboundMessage message) throws IOException, InterruptedException {
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
            }
            /*if (isSurveyInProgress.containsKey(senderId) && isSurveyInProgress.get(senderId)) {
                int currentQuestion = currentSurveyQuestion.get(senderId);

                if (currentQuestion == 6) {
                    handleSurveyResponse6(senderId, text);
                    whatsAppService.sendTextMessage(senderId, "¡Gracias por completar la encuesta!");
                    isSurveyInProgress.put(senderId, false);
                    isSurveyInProgress.remove(senderId);
                    currentSurveyQuestion.remove(senderId);
                }
            }*/
            else {
                // Verificar si el usuario tiene una relación activa con un asesor
                RelationAdviserCustomer relacionAsesorCliente = relationAdviserCustomerService.encontrarConversacionesActivas(senderId, true);
                if (relacionAsesorCliente != null) {
                    // Reenviar el mensaje al asesor
                    //whatsAppService.sendTextMessage(relacionAsesorCliente.getAdviserNumber(), "Mensaje del usuario " + senderId + ": " + text); //por ahora no se reenvia
                } else {
                    String textasesor = textMessage.getText().toUpperCase();
                    if (textasesor.equals("ASESOR")) {
                        Optional<String> asesorDisponible = relationAdviserCustomerService.buscarAsesorDisponible();
                        if (asesorDisponible.isPresent()) {
                            String asesorNumber = asesorDisponible.get();
                            String conversationSummary = chatGptService.generateConversationSummary(senderId);
                            String customerName = getCustomerName(senderId);
                            String messageToAdvisor = "Atención: El cliente " + customerName + " (Número de celular: " + senderId + ") ha solicitado un asesor.\n\n" +
                                    "Resumen de la conversación:\n" + conversationSummary;
                            whatsAppService.sendTextMessage(asesorNumber, messageToAdvisor);
                            whatsAppService.sendTextMessage(senderId, "Estamos comunicando a nuestros asesores para que se puedan conectar al chat. Por favor, espera un momento.");
                            guardarRelacionAsesorCliente(senderId, asesorNumber);

                            // Desactivar el procesamiento de mensajes para este usuario y marcar que está esperando al asesor
                            isFirstMessage.put(senderId, false);
                            isWaitingForAdvisor.put(senderId, true);
                        } else {
                            whatsAppService.sendTextMessage(senderId, "Lo sentimos, no hay asesores disponibles en este momento. Por favor, intente más tarde.");
                        }
                    } else {
                        String analyzedResponse = sentimentAnalysisService.analyzeTextAndSaveEmotions(text);
                        JsonObject jsonObject = JsonParser.parseString(analyzedResponse).getAsJsonObject();
                        String emotion = jsonObject.get("sentimiento_predominante").getAsString();

                        ChatMessage chatMessage = chatGptService.getChatCompletion(senderId, text, emotion);
                        String responseText = chatMessage.getContent();
                        whatsAppService.sendTextMessage(senderId, responseText);
                        // Verificar si el mensaje del cliente indica que ha resuelto su consulta o no necesita más ayuda
                        if (chatGptService.isResolutionMessage(text)) {
                            // Aquí inicia la encuesta
                            sendSurveyQuestion1(senderId);
                            isSurveyInProgress.put(senderId, true);
                            currentSurveyQuestion.put(senderId, 1);
                        }
                    }
                }

            }

        } else if (messageType.equals("VOICE")) {
            WhatsAppWebhookInboundVoiceMessage voiceMessage = (WhatsAppWebhookInboundVoiceMessage) message;
            String voiceUrl = voiceMessage.getUrl();
            // Descargar el audio desde la URL y guardar con extensión MP3
            File mp3File = audioDownloader.downloadAudio(voiceUrl);
            // Transcribir el audio a texto
            String transcription = transcriptionService.transcribeAudio(mp3File);
            ChatMessage chatMessage = chatGptService.getChatCompletion(senderId, transcription, "");
            String responseText = chatMessage.getContent();
            whatsAppService.sendTextMessage(senderId, responseText);
        } else if (messageType.equals("INTERACTIVE_LIST_REPLY")) {
            WhatsAppWebhookListReplyContent interactiveMessage = (WhatsAppWebhookListReplyContent)message;
            String selectedOptionId = interactiveMessage.getId();
            if (isSurveyInProgress.containsKey(senderId) && isSurveyInProgress.get(senderId)) {
                int currentQuestion = currentSurveyQuestion.get(senderId);

                switch (currentQuestion) {
                    case 1:
                        handleSurveyResponse1(senderId, selectedOptionId);
                        sendSurveyQuestion2(senderId);
                        currentSurveyQuestion.put(senderId, 2);
                        break;
                    case 2:
                        handleSurveyResponse2(senderId, selectedOptionId);
                        sendSurveyQuestion3(senderId);
                        currentSurveyQuestion.put(senderId, 3);
                        break;
                    case 3:
                        handleSurveyResponse3(senderId, selectedOptionId);
                        sendSurveyQuestion4(senderId);
                        currentSurveyQuestion.put(senderId, 4);
                        break;
                    case 4:
                        handleSurveyResponse4(senderId, selectedOptionId);
                        sendSurveyQuestion5(senderId);
                        currentSurveyQuestion.put(senderId, 5);
                        break;
                    case 5:
                        handleSurveyResponse5(senderId, selectedOptionId);
                        sendSurveyQuestion6(senderId);
                        currentSurveyQuestion.put(senderId, 6);
                        break;
                    case 6:
                        handleSurveyResponse6(senderId, selectedOptionId);
                        whatsAppService.sendTextMessage(senderId, "¡Gracias por completar la encuesta!");
                        isSurveyInProgress.put(senderId, false);
                        isSurveyInProgress.remove(senderId);
                        currentSurveyQuestion.remove(senderId);
                }
            }
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
                    return userData;
                }
            }
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

    private String getCustomerName(String senderId) {
        Optional<Relation> relationOptional = relationRepository.findByUserNumber(senderId);
        if (relationOptional.isPresent()) {
            Relation relation = relationOptional.get();
            return relation.getName();
        }
        return "";
    }

    private Map<String, String> createOption(String id, String title) {
        Map<String, String> optionMap = new HashMap<>();
        optionMap.put("id", id);
        optionMap.put("title", title);
        return optionMap;
    }

    private void sendInteractiveListMessage(String customerNumber, String question, List<Map<String, String>> options) {
        List<Map<String, String>> rows = new ArrayList<>();
        for (Map<String, String> option : options) {
            Map<String, String> row = new HashMap<>();
            row.put("id", option.get("title"));
            row.put("title", option.get("title"));
            rows.add(row);
        }
        whatsAppService.sendInteractiveListMessage(customerNumber, question, rows);
    }


    // Métodos para enviar preguntas individuales de la encuesta
    private void sendSurveyQuestion1(String customerNumber) {
        List<Map<String, String>> options = new ArrayList<>();
        options.add(createOption("1", "1. Nada de precisión"));
        options.add(createOption("2", "2. Poca precisión"));
        options.add(createOption("3", "3. Moderada precisión"));
        options.add(createOption("4", "4. Mucha precisión"));
        options.add(createOption("5", "5. Completa precisión"));
        sendInteractiveListMessage(customerNumber, "¿En qué medida el chatbot logró comprender con precisión tus preguntas y dudas?", options);
    }
    private void sendSurveyQuestion2(String customerNumber) {
        List<Map<String, String>> options = new ArrayList<>();
        options.add(createOption("1", "1. Nada interactivas"));
        options.add(createOption("2", "2. Poco interactivas"));
        options.add(createOption("3", "3. Moderadas"));
        options.add(createOption("4", "4. Muy interactivas"));
        options.add(createOption("5", "5. Completamente int."));
        sendInteractiveListMessage(customerNumber, "¿Sientes que las respuestas del chatbot fueron interactivas y fluidas en el diálogo?", options);
    }
    private void sendSurveyQuestion3(String customerNumber) {
        List<Map<String, String>> options = new ArrayList<>();
        options.add(createOption("1", "1. Nada naturales"));
        options.add(createOption("2", "2. Poco naturales"));
        options.add(createOption("3", "3. Moderadamente nat."));
        options.add(createOption("4", "4. Muy naturales"));
        options.add(createOption("5", "5. Completamente nat."));
        sendInteractiveListMessage(customerNumber, "¿En qué medida las respuestas del chatbot te parecieron naturales y similares a las de una persona?", options);
    }
    private void sendSurveyQuestion4(String customerNumber) {
        List<Map<String, String>> options = new ArrayList<>();
        options.add(createOption("1", "1. Nada confiable"));
        options.add(createOption("2", "2. Poco confiable"));
        options.add(createOption("3", "3. Moderadamente conf."));
        options.add(createOption("4", "4. Muy confiable"));
        options.add(createOption("5", "5. Completamente conf."));
        sendInteractiveListMessage(customerNumber, "¿Qué tan confiable te parece el chatbot en general?", options);
    }
    private void sendSurveyQuestion5(String customerNumber) {
        List<Map<String, String>> options = new ArrayList<>();
        options.add(createOption("1", "1. Nada positiva"));
        options.add(createOption("2", "2. Poco positiva"));
        options.add(createOption("3", "3. Moderadamente pos."));
        options.add(createOption("4", "4. Muy positiva"));
        options.add(createOption("5", "5. Extremadamente pos."));
        sendInteractiveListMessage(customerNumber, "¿En qué medida tu experiencia general con el chatbot ha sido positiva?", options);
    }
    private void sendSurveyQuestion6(String customerNumber) {
        List<Map<String, String>> options = new ArrayList<>();
        options.add(createOption("1", "1. Nada rápido ni efec."));
        options.add(createOption("2", "2. Poco rápido y efec."));
        options.add(createOption("3", "3. Moderadamente r&a."));
        options.add(createOption("4", "4. Muy rápido y efec."));
        options.add(createOption("5", "5. Completamente r&a."));
        sendInteractiveListMessage(customerNumber, "¿Consideras que el chatbot resolvió tus consultas de manera rápida y efectiva?", options);
    }
    private void handleSurveyResponse1(String customerNumber, String selectedOption) {
        // Extraer el número de la opción seleccionada
        String responseNumber = selectedOption.substring(0, 1);
        // Guardar la respuesta de la pregunta 1
        SurveyResponse surveyResponse = new SurveyResponse();
        surveyResponse.setCustomerNumber(customerNumber);
        surveyResponse.setQuestionNumber(1);
        surveyResponse.setQuestionMetric("comprensión humana");
        surveyResponse.setResponse(responseNumber);
        surveyResponse.setCreatedAt(LocalDateTime.now());
        surveyResponseService.saveSurveyResponse(surveyResponse);
    }
    private void handleSurveyResponse2(String customerNumber, String selectedOption) {
        // Extraer el número de la opción seleccionada
        String responseNumber = selectedOption.substring(0, 1);
        // Guardar la respuesta de la pregunta 2
        SurveyResponse surveyResponse = new SurveyResponse();
        surveyResponse.setCustomerNumber(customerNumber);
        surveyResponse.setQuestionNumber(2);
        surveyResponse.setQuestionMetric("contingencia percibida");
        surveyResponse.setResponse(responseNumber);
        surveyResponse.setCreatedAt(LocalDateTime.now());
        surveyResponseService.saveSurveyResponse(surveyResponse);
    }
    private void handleSurveyResponse3(String customerNumber, String selectedOption) {
        // Extraer el número de la opción seleccionada
        String responseNumber = selectedOption.substring(0, 1);
        // Guardar la respuesta de la pregunta 3
        SurveyResponse surveyResponse = new SurveyResponse();
        surveyResponse.setCustomerNumber(customerNumber);
        surveyResponse.setQuestionNumber(3);
        surveyResponse.setQuestionMetric("humanidad de la respuesta");
        surveyResponse.setResponse(responseNumber);
        surveyResponse.setCreatedAt(LocalDateTime.now());
        surveyResponseService.saveSurveyResponse(surveyResponse);
    }
    private void handleSurveyResponse4(String customerNumber, String selectedOption) {
        // Extraer el número de la opción seleccionada
        String responseNumber = selectedOption.substring(0, 1);
        // Guardar la respuesta de la pregunta 4
        SurveyResponse surveyResponse = new SurveyResponse();
        surveyResponse.setCustomerNumber(customerNumber);
        surveyResponse.setQuestionNumber(4);
        surveyResponse.setQuestionMetric("confiabilidad del Chatbot");
        surveyResponse.setResponse(responseNumber);
        surveyResponse.setCreatedAt(LocalDateTime.now());
        surveyResponseService.saveSurveyResponse(surveyResponse);
    }
    private void handleSurveyResponse5(String customerNumber, String selectedOption) {
        // Extraer el número de la opción seleccionada
        String responseNumber = selectedOption.substring(0, 1);
        // Guardar la respuesta de la pregunta 5
        SurveyResponse surveyResponse = new SurveyResponse();
        surveyResponse.setCustomerNumber(customerNumber);
        surveyResponse.setQuestionNumber(5);
        surveyResponse.setQuestionMetric("experiencia del cliente");
        surveyResponse.setResponse(responseNumber);
        surveyResponse.setCreatedAt(LocalDateTime.now());
        surveyResponseService.saveSurveyResponse(surveyResponse);
    }
    private void handleSurveyResponse6(String customerNumber, String selectedOption) {
        // Extraer el número de la opción seleccionada
        String responseNumber = selectedOption.substring(0, 1);
        // Guardar la respuesta de la pregunta 6
        SurveyResponse surveyResponse = new SurveyResponse();
        surveyResponse.setCustomerNumber(customerNumber);
        surveyResponse.setQuestionNumber(6);
        surveyResponse.setQuestionMetric("tasa de resolución en el primer contacto");
        surveyResponse.setResponse(responseNumber);
        surveyResponse.setCreatedAt(LocalDateTime.now());
        surveyResponseService.saveSurveyResponse(surveyResponse);
    }
}
