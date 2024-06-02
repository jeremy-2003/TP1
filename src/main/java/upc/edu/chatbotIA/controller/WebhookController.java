package upc.edu.chatbotIA.controller;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobip.model.*;

import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import upc.edu.chatbotIA.model.*;
import upc.edu.chatbotIA.repository.AdviserRepository;
import upc.edu.chatbotIA.service.*;
import upc.edu.chatbotIA.util.AudioDownloader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
@Transactional
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

    private final RelationService relationService;
    private final BlockedUserService blockedUserService;
    private final AdviserRepository adviserRepository;

    private final SurveyResponseService surveyResponseService;
    private final ChatInteractionMetricsService chatInteractionMetricsService;
    private final TicketService ticketService;
    private final AppointmentsService appointmentsService;
    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private Map<String, String> conversationState = new HashMap<>();
    private Map<String, String> selectedDayMap = new HashMap<>();
    private Map<String, String> selectedTimeMap = new HashMap<>();
    private Map<String, Boolean> isWaitingForVisitReason = new HashMap<>();
    private Map<String, String> selectedVisitReasonMap = new HashMap<>();
    private Map<String, Boolean> isFirstMessage = new HashMap<>();
    private Map<String, Boolean> isTicketCreationInProgress = new HashMap<>();
    private Map<String, Boolean> isWaitingForAdvisor = new HashMap<>();
    private Map<String, Boolean> isSurveyInProgress = new HashMap<>();
    private Map<String, Integer> currentSurveyQuestion = new HashMap<>();

    @Autowired
    public WebhookController(ObjectMapper objectMapper, AudioDownloader audioDownloader,
                             TranscriptionService transcriptionService, ChatGptService chatGptService, WhatsAppService whatsAppService,
                             SheetsService sheetsService, RelationService relationService, BlockedUserService blockedUserService,
                             RelationAdviserCustomerService relationAdviserCustomerService, SentimentAnalysisService sentimentAnalysisService,
                             SurveyResponseService surveyResponseService, AdviserRepository adviserRepository, ChatInteractionMetricsService chatInteractionMetricsService,
                             TicketService ticketService, AppointmentsService appointmentsService) {
        this.objectMapper = objectMapper;
        this.audioDownloader = audioDownloader;
        this.transcriptionService = transcriptionService;
        this.chatGptService = chatGptService;
        this.whatsAppService = whatsAppService;
        this.sheetsService = sheetsService;
        this.relationService = relationService;
        this.blockedUserService = blockedUserService;
        this.relationAdviserCustomerService = relationAdviserCustomerService;
        this.sentimentAnalysisService = sentimentAnalysisService;
        this.surveyResponseService = surveyResponseService;
        this.adviserRepository = adviserRepository;
        this.chatInteractionMetricsService = chatInteractionMetricsService;
        this.ticketService = ticketService;
        this.appointmentsService = appointmentsService;
    }

    @PostMapping("/incoming-whatsapp")
    public ResponseEntity<Void> receiveWhatsApp(@RequestBody String requestBody) {
        try {
            WhatsAppWebhookInboundMessageResult messages = objectMapper.readValue(requestBody,
                    WhatsAppWebhookInboundMessageResult.class);
            for (WhatsAppWebhookInboundMessageData messageData : messages.getResults()) {
                String senderId = messageData.getFrom();
                // Verificar si el remitente es un asesor
                Optional<ChatInteractionMetrics> existingInteraction = chatInteractionMetricsService.findActiveInteraction(senderId);
                ChatInteractionMetrics interaction;
                if (existingInteraction.isPresent()) {
                    interaction = existingInteraction.get();
                } else {
                    interaction = chatInteractionMetricsService.startInteraction(senderId);
                }
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
                                String welcomeBackMessage = "\uD83D\uDCE1 Ya estás de regreso conmigo, soy TeleBuddy. \uD83D\uDE0A Espero que la atención con nuestro asesor haya sido útil. ¿Hay algo más en lo que pueda asistirte hoy?";
                                whatsAppService.sendTextMessage(customerNumber, welcomeBackMessage);
                                Relation relation = relationService.findByUserNumber(customerNumber).orElse(null);
                                if (relation != null) {
                                    LocalDateTime now = LocalDateTime.now();
                                    relation.setLastInteractionTime(LocalDateTime.now());
                                    LocalDateTime expirationThreshold = relation.getExpirationTime().minusDays(5); // Umbral de extensión de 5 días
                                    if (relation.getLastInteractionTime().isAfter(expirationThreshold)) {
                                        relation.setExpirationTime(now.plusDays(30)); // Extender la fecha de expiración por 30 días más
                                    }
                                    relationService.save(relation);
                                }
                                whatsAppService.sendTextMessage(senderId, "Has finalizado la conversación con el cliente de número " + customerNumber + " y nombre " + relation.getName());

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
                Optional<Relation> existingRelation = relationService.findByUserNumber(senderId);
                if (blockedUserService.isUserBlocked(senderId)) {
                    whatsAppService.sendTextMessage(senderId, "\uD83D\uDEAB Estás temporalmente bloqueado. Por favor, espera 5 minutos antes de intentar enviar mensajes nuevamente. ¡Gracias por tu paciencia!");
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
                        relationService.save(relation);
                    } else if (!relation.getActive() && relation.getExpirationTime().isAfter(now)) {
                        // Si la relación está inactiva pero no ha expirado, enviar un saludo personalizado y procesar el mensaje normalmente
                        long startTime = System.currentTimeMillis();
                        String welcomeMessage = "Hola " + relation.getName() + ", ¡Bienvenido de nuevo! 😃 Indicame en que puedo ayudarte esta vez.";
                        whatsAppService.sendTextMessage(senderId, welcomeMessage);
                        // Actualizar la marca de tiempo de interacción y reactivar la relación
                        long endTime = System.currentTimeMillis();
                        long responseTime = endTime - startTime;
                        chatInteractionMetricsService.updateAverageResponseTime(interaction, responseTime);
                        relation.setLastInteractionTime(now);
                        relation.setExpirationTime(LocalDateTime.now().plusMinutes(30));
                        relation.setActive(true);
                        relationService.save(relation);
                    }
                    else {
                        // Si la relación ha expirado, eliminar la relación y solicitar el DNI nuevamente
                        relationService.delete(relation);
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
                                if (userData.equals("Vemos que no eres cliente nuestro. 🤔 ¿Podrías, por favor, brindarnos tu nombre para continuar con tu consulta? ✍️")) {
                                    whatsAppService.sendTextMessage(senderId, userData);
                                    isFirstMessage.put(senderId, false);
                                    conversationState.put(senderId, "AWAITING_NAME");
                                    saveRelation(senderId,"", "", "", "", false);
                                } else if (userData.equals("El DNI ingresado no es válido. 🚫 Por favor, ingresa un número de DNI de 8 dígitos.")) {
                                    whatsAppService.sendTextMessage(senderId, userData);
                                    // Mantener el estado de primer mensaje para volver a solicitar el DNI
                                    isFirstMessage.put(senderId, true);
                                }
                                else {
                                    whatsAppService.sendTextMessage(senderId, userData);
                                    isFirstMessage.put(senderId, false);
                                }
                            } else {
                                String state = conversationState.get(senderId);
                                if (state != null && state.equals("AWAITING_NAME")) {
                                    String name = text;
                                    whatsAppService.sendTextMessage(senderId, "Gracias, " + name + ". 😊 Cuéntanos, ¿en qué podemos ayudarte?");
                                    Optional<Relation> relationToUpdate = relationService.findByUserNumber(senderId);
                                    if (relationToUpdate.isPresent()) {
                                        Relation relation = relationToUpdate.get();
                                        relation.setName(name);
                                        relationService.save(relation);
                                        conversationState.remove(senderId);
                                        isFirstMessage.remove(senderId);
                                    }
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
        Optional<ChatInteractionMetrics> existingInteraction = chatInteractionMetricsService.findActiveInteraction(senderId);
        ChatInteractionMetrics interaction;
        if (existingInteraction.isPresent()) {
            interaction = existingInteraction.get();
        } else {
            interaction = chatInteractionMetricsService.startInteraction(senderId);
        }

        String messageType = String.valueOf(message.getType());
        if (messageType.equals("TEXT")) {
            WhatsAppWebhookInboundTextMessage textMessage = (WhatsAppWebhookInboundTextMessage) message;
            String text = textMessage.getText();

            if (isFirstMessage.containsKey(senderId) && isFirstMessage.get(senderId)) {
                String userData = searchDniInExcel(senderId, text);
                if (userData.equals("El DNI ingresado no es válido. 🚫 Por favor, ingresa un número de DNI de 8 dígitos.")) {
                    whatsAppService.sendTextMessage(senderId, userData);
                    // Mantener el estado de primer mensaje para volver a solicitar el DNI
                    isFirstMessage.put(senderId, true);
                } else {
                    whatsAppService.sendTextMessage(senderId, userData);
                    isFirstMessage.put(senderId, false);
                }
            } else if (isWaitingForVisitReason.containsKey(senderId) && isWaitingForVisitReason.get(senderId)) {
                String visitReason = text;
                selectedVisitReasonMap.put(senderId, visitReason);
                // Clasificar el motivo de la visita
                String classifiedReason = chatGptService.classifyVisitReason(visitReason);
                String selectedDay = selectedDayMap.get(senderId);
                String selectedTime = selectedTimeMap.get(senderId);
                LocalDateTime visitDate = LocalDateTime.parse(selectedDay + "T" + selectedTime);
                Optional<Relation> relationOptional = relationService.findByUserNumber(senderId);
                Relation relation = relationOptional.get();
                String ruc = relation.getRuc();
                String confirmationMessage = "✅ Su cita ha sido agendada para el día " + selectedDay + " a las " + selectedTime + " por el motivo de " + classifiedReason + ".";
                whatsAppService.sendTextMessage(senderId, confirmationMessage);
                appointmentsService.scheduleAppointment(ruc, classifiedReason, visitDate, "");

                isWaitingForVisitReason.put(senderId, false);
                selectedDayMap.remove(senderId);
                selectedTimeMap.remove(senderId);
                selectedVisitReasonMap.remove(senderId);
            } else {
                // Verificar si el usuario tiene una relación activa con un asesor
                RelationAdviserCustomer relacionAsesorCliente = relationAdviserCustomerService.encontrarConversacionesActivas(senderId, true);
                if (relacionAsesorCliente != null) {
                    // Reenviar el mensaje al asesor
                    // whatsAppService.sendTextMessage(relacionAsesorCliente.getAdviserNumber(), "Mensaje del usuario " + senderId + ": " + text); //por ahora no se reenvia
                } else {
                    String textasesor = textMessage.getText().toUpperCase();
                    if (textasesor.equals("ASESOR")) {
                        Optional<String> asesorDisponible = relationAdviserCustomerService.buscarAsesorDisponible();
                        if (asesorDisponible.isPresent()) {
                            String asesorNumber = asesorDisponible.get();
                            String conversationSummary = chatGptService.generateConversationSummary(senderId);
                            String customerName = getCustomerName(senderId);
                            String messageToAdvisor = "🔔 Atención: El cliente " + customerName + " (Número de celular: " + senderId + ") ha solicitado un asesor.\n\n" +
                                    "📋 Resumen de la conversación:\n" + conversationSummary + ".";
                            whatsAppService.sendTextMessage(asesorNumber, messageToAdvisor);
                            whatsAppService.sendTextMessage(senderId, "Estamos comunicando a nuestros asesores para que se puedan conectar al chat. ⏳ Por favor, espera un momento.");
                            guardarRelacionAsesorCliente(senderId, asesorNumber);

                            // Desactivar el procesamiento de mensajes para este usuario y marcar que está esperando al asesor
                            isFirstMessage.put(senderId, false);
                            isWaitingForAdvisor.put(senderId, true);

                            chatInteractionMetricsService.markInteractionAsRequestedAdvisor(interaction);
                        } else {
                            whatsAppService.sendTextMessage(senderId, "Lamentamos informarte que no hay asesores disponibles en este momento. 😔 Pero no te preocupes, cuéntame qué necesitas y haré todo lo posible para asistirte. ¡Estamos aquí para ayudarte!");
                        }
                    } else {
                        if (isTicketCreationInProgress.containsKey(senderId) && isTicketCreationInProgress.get(senderId)) {
                            // Procesar el mensaje como descripción del problema
                            Optional<Relation> relationObtain = relationService.findByUserNumber(senderId);
                            Relation relation = relationObtain.get();
                            String userId = relation.getUserId().toString();
                            String description = text;
                            String urgency = chatGptService.evaluateUrgency(text);
                            ticketService.createNewTicket(userId, description, urgency, "PENDIENTE");
                            isTicketCreationInProgress.put(senderId, false); // Limpiar el estado de espera de descripción
                            String empatheticResponse = chatGptService.generateEmpatheticResponse(description);
                            whatsAppService.sendTextMessage(senderId, " Tu ticket ha sido creado con éxito! Un asesor se pondrá en contacto contigo pronto. 😊"); // Enviar id de ticket
                        } else if (chatGptService.isTicketCreationRequired(text)) {
                            Optional<Relation> relationObtain = relationService.findByUserNumber(senderId);
                            Relation relation = relationObtain.get();
                            if (relation.getUserId() != null) {
                                isTicketCreationInProgress.put(senderId, true);
                                whatsAppService.sendTextMessage(senderId, "¿Podrías, por favor, indicar la descripción del problema? 📝");
                            }/*else{
                        whatsAppService.sendTextMessage(senderId, "Lo sentimos, como no eres ningun cliente no puedes generar tickets");
                    }*/
                        } else if(chatGptService.isAppointmentSchedulingRequired(text)){
                            List<LocalDateTime> availableDates = appointmentsService.checkAvailableDates();
                            Map<LocalDate, List<LocalDateTime>> datesByDay = availableDates.stream()
                                    .collect(Collectors.groupingBy(LocalDateTime::toLocalDate));

                            // Crear una lista de días disponibles con formato amigable
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", new Locale("es", "ES"));
                            List<Map<String, String>> daysOptions = new ArrayList<>();
                            for (LocalDate date : datesByDay.keySet()) {
                                Map<String, String> option = new HashMap<>();
                                option.put("id", date.toString());
                                option.put("title", date.format(formatter));
                                daysOptions.add(option);
                            }

                            // Enviar la lista de días disponibles al usuario
                            whatsAppService.sendListMessage(senderId, "📅 Por favor, selecciona un día para tu cita:", daysOptions);
                        }
                        else {
                            String analyzedResponse = sentimentAnalysisService.analyzeTextAndSaveEmotions(text);
                            JsonObject jsonObject = JsonParser.parseString(analyzedResponse).getAsJsonObject();
                            String emotion = jsonObject.get("sentimiento_predominante").getAsString();
                            long startTime = System.currentTimeMillis();
                            ChatMessage chatMessage = chatGptService.getChatCompletion(senderId, text, emotion);
                            String responseText = chatMessage.getContent();
                            whatsAppService.sendTextMessage(senderId, responseText);
                            long endTime = System.currentTimeMillis();
                            long responseTime = endTime - startTime;
                            chatInteractionMetricsService.updateAverageResponseTime(interaction, responseTime);
                            if (chatGptService.isResolutionMessage(text)) {
                                // Verificar si el usuario no ha sido atendido por un asesor
                                if (!interaction.isRequestedAdvisor()) {
                                    chatInteractionMetricsService.markInteractionAsResolvedInFirstContact(interaction);
                                }
                                // Aquí inicia la encuesta
                                whatsAppService.sendTextMessage(senderId, "Antes de terminar, ayúdanos con esta encuesta para seguir mejorando 📝");
                                sendSurveyQuestion1(senderId);
                                isSurveyInProgress.put(senderId, true);
                                currentSurveyQuestion.put(senderId, 1);
                            }
                        }
                    }
                }
            }
        } else if (messageType.equals("AUDIO")) {
            long startTime = System.currentTimeMillis();
            WhatsAppWebhookInboundAudioMessage voiceMessage = (WhatsAppWebhookInboundAudioMessage) message;
            String voiceUrl = voiceMessage.getUrl();
            // Descargar el audio desde la URL y guardar con extensión MP3
            File mp3File = audioDownloader.downloadAudio(voiceUrl);
            // Transcribir el audio a texto
            String transcription = transcriptionService.transcribeAudio(mp3File);
            ChatMessage chatMessage = chatGptService.getChatCompletion(senderId, transcription, "");
            String responseText = chatMessage.getContent();
            whatsAppService.sendTextMessage(senderId, responseText);
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            chatInteractionMetricsService.updateAverageResponseTime(interaction, responseTime);
        } else if (messageType.equals("INTERACTIVE_LIST_REPLY")) {
            WhatsAppWebhookListReplyContent interactiveMessage = (WhatsAppWebhookListReplyContent) message;
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
                        whatsAppService.sendTextMessage(senderId, "🎉 ¡Gracias por completar la encuesta! Tu opinión es muy importante para nosotros. 😊 \n Recuerda que puedes escribirme las 24 horas del día. ¡Hasta pronto! \uD83D\uDC4B\n");
                        isSurveyInProgress.put(senderId, false);
                        isSurveyInProgress.remove(senderId);
                        currentSurveyQuestion.remove(senderId);
                        chatInteractionMetricsService.endInteraction(interaction);
                        System.out.println("Buscando la relación para el usuario: " + senderId);
                        Relation relation = relationService.findByUserNumber(senderId)
                                .orElseThrow(() -> new RuntimeException("No se encontró la relación para el usuario: " + senderId));
                        System.out.println("Relación encontrada: " + relation);

                        // Log antes de cambiar el estado
                        System.out.println("Cambiando el estado de la relación a inactivo.");
                        relation.setActive(false);

                        // Guardar y verificar
                        Relation savedRelation = relationService.save(relation);
                        System.out.println("Relación guardada: " + savedRelation);
                        break;
                }
            } else {
                System.out.println("Ingreso aqui en seleccionar horario");
                String selectedDay = interactiveMessage.getId();
                handleDaySelection(senderId, selectedDay);
            }
        } else if (messageType.equals("INTERACTIVE_BUTTON_REPLY")) {
            WhatsAppWebhookButtonReplyContent interactiveMessage = (WhatsAppWebhookButtonReplyContent) message;
            String selectedTime = interactiveMessage.getId();
            handleTimeSelection(senderId, selectedTime);
        }
    }


    private void sendWelcomeMessage(String senderId) {
        // Envía un mensaje de bienvenida y solicita el DNI
        String welcomeMessage = "¡Hola! Soy TeleBuddy, tu asistente virtual de SOE Industrial E.I.R.L. 😊 " +
                "Estoy aquí para ayudarte con cualquier consulta sobre nuestros servicios del sector telecomunicaciones. \n Para comenzar, por favor ingresa tu número de DNI:";
        whatsAppService.sendTextMessage(senderId, welcomeMessage);
    }

    private String searchDniInExcel(String senderId, String dni) {
        String range = "DB_USURIOS!A1:F";
        try {
            if (!dni.matches("\\d{8}")) {
                int attempts = failedAttempts.getOrDefault(senderId, 0) + 1;
                failedAttempts.put(senderId, attempts);

                // Verificar si se supera el límite de intentos fallidos
                if (attempts >= 3) {
                    blockedUserService.blockUser(senderId);
                    failedAttempts.remove(senderId);
                    return "🚫 Has sido bloqueado temporalmente debido a múltiples intentos fallidos. Por favor, intenta nuevamente en unos minutos. ¡Gracias por tu paciencia!";
                }
                return "🚫 El DNI ingresado no es válido. Por favor, ingresa un número de DNI de 8 dígitos.";
            }

            List<List<Object>> excelData = sheetsService.connectToGoogleSheets(range);
            for (List<Object> row : excelData) {
                if (row.size() > 4 && row.get(4) instanceof String && row.get(3).equals(dni)) {
                    String userId = row.get(0).toString();
                    String name = row.get(1) + " " + row.get(2);
                    String ruc = row.get(4).toString();
                    String companyName = row.get(5).toString();
                    saveRelation(senderId, userId, name, ruc, companyName, true);
                    String userData = "¡Hola " + name + "! 😊 Es un gusto que te comuniques con nosotros. " +
                            "Aquí puedes registrar tickets de problemas, consultar sobre nuestros servicios, obtener información " +
                            "y recibir apoyo con cualquier inconveniente relacionado con nuestros servicios de telecomunicaciones. " +
                            "¿En qué podemos ayudarte?";
                    return userData;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Vemos que no eres cliente nuestro. 🤔 ¿Podrías, por favor, brindarnos tu nombre para continuar con tu consulta? ✍️";
    }

    private void saveRelation(String senderId, String userId, String name, String ruc, String companyName, Boolean client) {
        Relation relation = new Relation();
        relation.setUserNumber(senderId);
        if (!userId.isEmpty()) {
            relation.setUserId(Long.parseLong(userId));
        }
        relation.setName(name);
        relation.setRuc(ruc);
        relation.setCompany(companyName);
        relation.setExpirationTime(LocalDateTime.now().plusMinutes(30));
        relation.setActive(true);
        relation.setLastInteractionTime(LocalDateTime.now());
        relation.setClient(client);
        relationService.save(relation);
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
        Optional<Relation> relationOptional = relationService.findByUserNumber(senderId);
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
        options.add(createOption("1", "1. Nada positiva"));
        options.add(createOption("2", "2. Poco positiva"));
        options.add(createOption("3", "3. Moderadamente pos."));
        options.add(createOption("4", "4. Muy positiva"));
        options.add(createOption("5", "5. Extremadamente pos."));
        sendInteractiveListMessage(customerNumber, "¿En qué medida tu experiencia general con el chatbot ha sido positiva?", options);
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
        surveyResponse.setQuestionMetric("experiencia del cliente");
        surveyResponse.setResponse(responseNumber);
        surveyResponse.setCreatedAt(LocalDateTime.now());
        surveyResponseService.saveSurveyResponse(surveyResponse);
    }

    public void handleDaySelection(String senderId, String selectedDay) {
        LocalDate day = LocalDate.parse(selectedDay);
        List<LocalDateTime> availableTimes = appointmentsService.checkAvailableDates().stream()
                .filter(date -> date.toLocalDate().equals(day))
                .collect(Collectors.toList());

        // Crear una lista de horarios disponibles para el día seleccionado
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        List<Map<String, String>> timeOptions = new ArrayList<>();
        for (LocalDateTime time : availableTimes) {
            Map<String, String> option = new HashMap<>();
            option.put("id", time.toLocalTime().format(timeFormatter));
            option.put("title", time.toLocalTime().format(timeFormatter));
            timeOptions.add(option);
        }

        // Guardar el día seleccionado en un mapa para el seguimiento del estado
        selectedDayMap.put(senderId, day.toString());

        // Enviar los botones de horarios disponibles al usuario
        whatsAppService.sendButtonMessage(senderId, "⏰ Por favor, selecciona un horario para tu cita:", timeOptions);
    }

    public void handleTimeSelection(String senderId, String selectedTime) {
        selectedTimeMap.put(senderId, selectedTime);
        whatsAppService.sendTextMessage(senderId, "✍️ Por favor, indícanos el motivo de agendar la visita:");
        isWaitingForVisitReason.put(senderId, true);
    }


}
