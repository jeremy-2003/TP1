package upc.edu.chatbotIA.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.*;
import upc.edu.chatbotIA.repository.ConversationRepository;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChatGptService {
    private final OpenAiService openAiService;
    private final ServiceProductService serviceProductService;
    private final AppointmentsService appointmentsService;
    private final ConversationRepository conversationRepository;
    private final RelationService relationService;
    private final SheetsService sheetsService;
    @Autowired
    public ChatGptService(OpenAiService openAiService, ConversationRepository conversationRepository, RelationService relationService,
                          SheetsService sheetsService, ServiceProductService serviceProductService, AppointmentsService appointmentsService) {
        this.openAiService = openAiService;
        this.conversationRepository = conversationRepository;
        this.relationService = relationService;
        this.sheetsService = sheetsService;
        this.serviceProductService = serviceProductService;
        this.appointmentsService = appointmentsService;
    }

    public ChatMessage getChatCompletion(String userId, String userMessage, String emotion) {
        // Recuperar las conversaciones anteriores del usuario
        List<Conversation> previousConversations = conversationRepository.findByUserNumberOrderByTimestampAsc(userId);
        Optional<Relation> relationOptional = relationService.findByUserNumber(userId);
        Relation relation = relationOptional.get();
        Long relationUserId = relation.getUserId();
        String ruc = relation.getRuc();
        // Verificar si relationUserId es null
        StringBuilder ticketsInfo = new StringBuilder();
        if (relationUserId == null) {
            ticketsInfo.append("El usuario no cuenta y no puede crear tickets porque no es cliente.\n");
        } else {
            // Obtener los tickets del usuario
            List<Ticket> tickets;
            try {
                tickets = sheetsService.getTicketsBySenderId(relationUserId.toString());
            } catch (IOException | GeneralSecurityException e) {
                // Manejar la excepción adecuadamente
                throw new RuntimeException("Error al obtener los tickets del usuario: " + relationUserId, e);
            }

            // Construir la información de los tickets para incluirla en el prompt
            if (!tickets.isEmpty()) {
                ticketsInfo.append("Tickets del usuario:\n");
                for (Ticket ticket : tickets) {
                    ticketsInfo.append("- ID: ").append(ticket.getId())
                            .append(", Descripción: ").append(ticket.getDescription())
                            .append(", Urgencia: ").append(ticket.getUrgency())
                            .append(", Estado: ").append(ticket.getStatus())
                            .append(", Fecha de creación: ").append(ticket.getUpdatedAt())
                            .append("\n");
                }
            } else {
                ticketsInfo.append("No se encontraron tickets para el usuario.\n");
            }
        }
        // Construir la información de los servicios activos para incluirla en el prompt
        StringBuilder activeServicesInfo = new StringBuilder();
        if (ruc != null) {
            // Obtener los servicios activos del cliente
            List<ServiceProduct> activeServices = serviceProductService.getServiceProductsByRucAndEstado(ruc);

            if (!activeServices.isEmpty()) {
                activeServicesInfo.append("Servicios activos del cliente:\n");
                for (ServiceProduct service : activeServices) {
                    activeServicesInfo.append("- Servicio: ").append(service.getNombre())
                            .append(", Velocidad: ").append(service.getVelocidad())
                            .append(", Precio: ").append(service.getPrecio())
                            .append(", Fecha de pago: ").append(service.getFechaPago())
                            .append("\n");
                }
            } else {
                activeServicesInfo.append("No se encontraron servicios activos para el cliente.\n");
            }
        } else {
            activeServicesInfo.append("El usuario no tiene ningún servicio porque no es cliente.\n");
        }

        // Construir la información de las citas pendientes para incluirla en el prompt
        StringBuilder appointmentsInfo = new StringBuilder();
        if (ruc != null) {
            // Obtener las citas pendientes del cliente
            List<Appointment> pendingAppointments = appointmentsService.getAppointmentsByRucAndEstado(ruc);

            if (!pendingAppointments.isEmpty()) {
                appointmentsInfo.append("Citas pendientes del cliente:\n");
                for (Appointment appointment : pendingAppointments) {
                    appointmentsInfo.append("- Servicio: ").append(appointment.getService())
                            .append(", Fecha de visita: ").append(appointment.getVisitDate())
                            .append(", Observación: ").append(appointment.getObservation())
                            .append("\n");
                }
            } else {
                appointmentsInfo.append("No se encontraron citas pendientes para el cliente.\n");
            }
        } else {
            appointmentsInfo.append("El usuario no tiene ninguna cita pendiente porque no es cliente.\n");
        }

        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setRole("system");
        systemMessage.setContent(
                "[INSTRUCCIONES]:\n" +
                        "- Actúa como un chatbot llamado 'TeleBuddy', encargado de la atención al cliente para servicios o proyectos de sector de Telecomunicaciones para la empresa Soe Industrial Eirl.\n" +
                        "- Responde utilizando únicamente la información proporcionada en la base de datos.\n" +
                        "Brinda información sobre los servicios de telecomunicaciones ofrecidos por SOE Industrial E.I.R.L.:\n" +
                        "  - Provisión de Internet de Banda Ancha: Ofrecemos servicios de internet de alta velocidad mediante tecnologías como fibra óptica y ADSL.\n" +
                        "    * Fibra Óptica:\n" +
                        "      - Plan 50 Mbps: S/ 70 mensuales\n" +
                        "      - Plan 100 Mbps: S/ 100 mensuales\n" +
                        "      - Plan 200 Mbps: S/ 150 mensuales\n" +
                        "    * ADSL:\n" +
                        "      - Plan 10 Mbps: S/ 65 mensuales\n" +
                        "      - Plan 20 Mbps: S/ 80 mensuales\n" +
                        "  - Planes de Internet Personalizados: Diversos planes de internet con diferentes velocidades y capacidades de datos para hogares y pequeñas empresas.\n" +
                        "    * Costos aproximados de S/ 100 a S/ 200 mensuales." +
                        "  - Instalación y Mantenimiento de Infraestructura de Redes: Diseño, instalación y mantenimiento de redes LAN y WAN para garantizar conectividad estable y de alta velocidad. Costos aproximados promedios: S/ 500 - S/ 2000, dependiendo del alcance del proyecto.\n" +
                        "  - Cableado Estructurado: Servicios de instalación de cableado estructurado para soportar redes de datos y telecomunicaciones. Costos aproximados promedios: S/ 300 - S/ 1500, según la complejidad de la instalación.\n" +
                        "  - Consultoría en Infraestructura de Redes (CIR): Asesoramiento para el diseño, implementación y mejora de infraestructuras de telecomunicaciones. Costos aproximados promedios: S/ 1000 - S/ 5000, dependiendo del alcance de la consultoría.\n" +
                        "  - Asesoría en Soluciones de Conectividad (ASC): Desarrollo de soluciones personalizadas para optimizar el uso de servicios de telecomunicaciones. Costos aproximados promedios: S/ 800 - S/ 3000, según las necesidades específicas del cliente.\n" +
                        "- Para contacto, compartir el número de WhatsApp: 994 283 802\n" +
                        "\n" +
                        "[OBLIGATORIO]:\n" +
                        "- Enfócate en responder de manera precisa y directa a la pregunta del usuario.\n" +
                        "- Proporciona la información más relevante para satisfacer la consulta del usuario.\n" +
                        "- Si la pregunta no está relacionada con los servicios o la información proporcionada, indica amablemente que no tienes información al respecto.\n" +
                        "- Limita tus respuestas a un máximo de 3 oraciones cortas y concisas.\n" +
                        "- Evita expandirte demasiado en detalles innecesarios y mantén las respuestas al punto.\n" +
                        "- Intenta solucionar cualquier dificultad que se presente al usuario si está en tu poder. Si se necesita a una persona técnica, recomienda el número de WhatsApp o indica que escriban 'asesor' para contactar con un asesor.\n" +
                        "\n" +
                        "[EMOCIÓN DETECTADA]: " + emotion + "\n" +
                        "\n" +
                        "[OBLIGATORIO]:\n" +
                        "- Al generar la respuesta, ten en cuenta la emoción detectada en el mensaje del cliente.\n" +
                        "- Adapta el tono y estilo de comunicación según la emoción identificada para brindar una respuesta más empática y acorde al estado emocional del cliente.\n" +
                        "\n" +
                        "[TICKETS DEL USUARIO]:\n" +
                        ticketsInfo.toString() +
                        "[SERVICIOS ACTIVOS DEL CLIENTE]:\n" +
                        activeServicesInfo.toString() +
                        "[CITAS PENDIENTES DEL CLIENTE]:\n" +
                        appointmentsInfo.toString()
        );
        // Construir la lista de mensajes de chat incluyendo el mensaje del sistema y el historial de conversación
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(systemMessage);

        for (Conversation conversation : previousConversations) {
            chatMessages.add(new ChatMessage("user", conversation.getResponse()));
        }

        // Añadir el mensaje actual del usuario
        chatMessages.add(new ChatMessage("user", userMessage));

        // Construir la solicitud de completado de chat
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(chatMessages)
                .model("gpt-4o-2024-05-13")
                .build();

        // Enviar la solicitud al servicio de OpenAI y obtener la respuesta
        ChatMessage assistantMessage = openAiService.createChatCompletion(completionRequest).getChoices().get(0).getMessage();

        // Guardar la conversación en la base de datos
        Conversation conversation = new Conversation();
        conversation.setUserNumber(userId);
        conversation.setPrompt(userMessage);
        conversation.setResponse(assistantMessage.getContent());
        conversation.setTimestamp(LocalDateTime.now());
        conversationRepository.save(conversation);

        return assistantMessage;
    }
    public String generateConversationSummary(String userNumber) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime tomorrow = today.plusDays(1);
        List<Conversation> conversations = conversationRepository.findByUserNumberAndTimestampBetweenOrderByTimestampAsc(userNumber, today, tomorrow);
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (Conversation conversation : conversations) {
            chatMessages.add(new ChatMessage("user", conversation.getPrompt()));
        }

        if (chatMessages.isEmpty()) {
            return "No hay conversaciones previas para generar un resumen.";
        }

        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setRole("system");
        systemMessage.setContent(
                "[INSTRUCCIONES]: Genera un resumen conciso de la conversación previa, enfocándote en los puntos clave y las solicitudes del usuario de existir un problema solo centrate en dar especificaciones de eso. " +
                        "El resumen debe ser breve y capturar la esencia de la conversación para que el asesor pueda entender rápidamente el contexto."
        );
        chatMessages.add(0, systemMessage);

        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(chatMessages)
                .model("gpt-3.5-turbo-0125")
                .build();

        ChatMessage summaryMessage = openAiService.createChatCompletion(completionRequest).getChoices().get(0).getMessage();

        return summaryMessage.getContent();
    }

    public boolean isResolutionMessage(String userMessage) {
        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setRole("system");
        systemMessage.setContent(
                "[INSTRUCCIONES]: Evalúa si la siguiente frase del cliente indica que su consulta ha sido resuelta o que ya no necesita más ayuda. " +
                        "[OBLIGATORIO]: Responde 'Sí' si la frase indica resolución o satisfacción con la respuesta recibida, o 'No' si la frase no indica resolución.\n\n" +
                        "[EJEMPLOS DE FRASES QUE INDICAN RESOLUCIÓN]: 'Gracias, eso era todo', 'Perfecto, solucionado', 'No necesito más ayuda, gracias', 'Todo claro, gracias'.\n\n" +
                        "[FRASE]: " + userMessage +
                        "\n\n[NOTA]: Si la frase solo menciona un agradecimiento sin indicar que la consulta está resuelta o que ya no necesita ayuda, responde 'No'."
        );
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(systemMessage);
        chatMessages.add(new ChatMessage("user", userMessage));
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(chatMessages)
                .model("gpt-3.5-turbo-0125")
                .build();
        ChatMessage assistantMessage = openAiService.createChatCompletion(completionRequest).getChoices().get(0).getMessage();
        return assistantMessage.getContent().trim().equalsIgnoreCase("Sí");
    }


    public boolean isTicketCreationRequired(String userMessage) {
        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setRole("system");
        systemMessage.setContent(
                "[INSTRUCCIONES]: Evalúa si el siguiente mensaje del cliente solicita explícitamente la creación de un nuevo ticket de soporte o reportar un problema.\n" +
                        "[OBLIGATORIO]: Responde únicamente 'Sí' si el mensaje indica claramente la necesidad de crear un nuevo ticket de soporte o reportar un problema, usando términos como 'crear un ticket', 'abrir un ticket', 'necesito un nuevo ticket', 'quiero registrar un ticket', 'quiero reportar un problema', 'necesito reportar un problema', 'quiero informar un problema'. Responde 'No' si el mensaje no requiere la creación de un nuevo ticket ni reportar un problema.\n" +
                        "[NOTA]: Si el mensaje del cliente solo menciona problemas o solicitudes de información sin pedir explícitamente la creación de un nuevo ticket o reportar un problema, o si se refiere a tickets ya existentes, responde 'No'.\n\n" +
                        "[MENSAJE DEL CLIENTE]: " + userMessage +
                        "\n\n[NOTA]: Si el mensaje del cliente solo solicita información sobre el estado de un ticket existente, responde 'No'."
        );

        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(systemMessage);
        chatMessages.add(new ChatMessage("user", userMessage));
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(chatMessages)
                .model("gpt-3.5-turbo-0125")
                .build();
        ChatMessage assistantMessage = openAiService.createChatCompletion(completionRequest).getChoices().get(0).getMessage();
        return assistantMessage.getContent().trim().equalsIgnoreCase("Sí");
    }

    public String evaluateUrgency(String userDescription) {
        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setRole("system");
        systemMessage.setContent(
                "[INSTRUCCIONES]: Evalúa la urgencia del siguiente problema descrito por el usuario y responde con una de las opciones: 'Alta', 'Media', 'Baja'.\n\n" +
                        "[OBLIGATORIO]: Solo debes dar como respuesta una de las opciones que te di a alegir sin agregar nada más.\n\n" +
                        "[DESCRIPCIÓN DEL PROBLEMA]: " + userDescription
        );

        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(systemMessage);

        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(chatMessages)
                .model("gpt-3.5-turbo-0125")
                .build();

        ChatMessage urgencyMessage = openAiService.createChatCompletion(completionRequest)
                .getChoices().get(0).getMessage();
        return urgencyMessage.getContent().trim().toUpperCase();
    }

    public boolean isAppointmentSchedulingRequired(String userMessage) {
        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setRole("system");
        systemMessage.setContent(
                "[INSTRUCCIONES]: Evalúa si el siguiente mensaje del cliente indica explícitamente la solicitud de agendar una cita de visita.\n" +
                        "[OBLIGATORIO]: Responde únicamente 'Sí' si el mensaje indica claramente la necesidad de agendar una cita de visita, usando términos como 'agendar una cita', 'programar una visita', 'necesito una cita', 'quiero una visita'. Responde 'No' si el mensaje no requiere una cita.\n" +
                        "[NOTA]: Si el mensaje del cliente solo menciona problemas o solicitudes de información sin pedir explícitamente una cita de visita, responde 'No'.\n\n" +
                        "[MENSAJE DEL CLIENTE]: " + userMessage
        );

        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(systemMessage);
        chatMessages.add(new ChatMessage("user", userMessage));
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(chatMessages)
                .model("gpt-3.5-turbo-0125")
                .build();
        ChatMessage assistantMessage = openAiService.createChatCompletion(completionRequest).getChoices().get(0).getMessage();
        return assistantMessage.getContent().trim().equalsIgnoreCase("Sí");
    }

    public String classifyVisitReason(String visitReason) {
        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setRole("system");
        systemMessage.setContent(
                "[INSTRUCCIONES]: Evalúa y clasifica el siguiente motivo de visita en una de las siguientes categorías: 'INTERNET FIBRA OPTICA'," +
                        "'INTERNET ADSL', 'INTERNET PERSONALIZADO', 'CONSULTORIA DE INFRAESTRUCTURA DE REDES', 'ASESORIA DE SOLUCIONES DE CONECTIVIDAD.'.\n" +
                        "[OBLIGATORIO]: Responde únicamente con la categoría más adecuada basada en el motivo de la visita proporcionado.\n" +
                        "[OBLIGATORIO] Solo debes de regresar la categoria sin agregar nada más." +
                        "[NOTA]: Si no puedes clasificar el motivo en ninguna de las categorías mencionadas, responde 'OTRO SERVICIO'.\n\n" +
                        "[MOTIVO DE LA VISITA]: " + visitReason
        );

        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(systemMessage);
        chatMessages.add(new ChatMessage("user", visitReason));
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(chatMessages)
                .model("gpt-3.5-turbo-0125")
                .build();
        ChatMessage assistantMessage = openAiService.createChatCompletion(completionRequest).getChoices().get(0).getMessage();
        return assistantMessage.getContent().trim();
    }


}
