package upc.edu.chatbotIA.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.Conversation;
import upc.edu.chatbotIA.model.Relation;
import upc.edu.chatbotIA.model.Ticket;
import upc.edu.chatbotIA.repository.ConversationRepository;
import upc.edu.chatbotIA.repository.RelationRepository;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChatGptService {
    private final OpenAiService openAiService;
    private final ConversationRepository conversationRepository;
    private final RelationRepository relationRepository;
    private final SheetsService sheetsService;
    @Autowired
    public ChatGptService(OpenAiService openAiService, ConversationRepository conversationRepository, RelationRepository relationRepository,
                          SheetsService sheetsService) {
        this.openAiService = openAiService;
        this.conversationRepository = conversationRepository;
        this.relationRepository = relationRepository;
        this.sheetsService = sheetsService;
    }

    public ChatMessage getChatCompletion(String userId, String userMessage, String emotion) {
        // Recuperar las conversaciones anteriores del usuario
        List<Conversation> previousConversations = conversationRepository.findByUserNumberOrderByTimestampAsc(userId);
        Optional<Relation> relationOptional = relationRepository.findByUserNumber(userId);
        Relation relation = relationOptional.get();
        Long relationUserId = relation.getUserId();

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
        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setRole("system");
        systemMessage.setContent(
                "[INSTRUCCIONES]:\n" +
                        "- Actúa como un chatbot llamado 'TeleBuddy', encargado de la atención al cliente para la empresa TelecomunicacionesCenter.\n" +
                        "- Responde utilizando únicamente la información proporcionada en la base de datos.\n" +
                        "- Brinda información sobre los servicios de cable e internet ofrecidos por TelecomunicacionesCenter:\n" +
                        "  - Planes de fibra óptica\n" +
                        "  - Planes DSL\n" +
                        "  - Cable satelital con 100 canales (extranjeros y nacionales)\n" +
                        "- Para contacto, compartir el número de WhatsApp: 994 283 802\n" +
                        "\n" +
                        "[OBLIGATORIO]:\n" +
                        "- Enfócate en responder de manera precisa y directa a la pregunta del usuario.\n" +
                        "- Proporciona la información más relevante para satisfacer la consulta del usuario.\n" +
                        "- Si la pregunta no está relacionada con los servicios o la información proporcionada, indica amablemente que no tienes información al respecto.\n" +
                        "- Limita tus respuestas a un máximo de 3 oraciones cortas y concisas.\n" +
                        "- Evita expandirte demasiado en detalles innecesarios y mantén las respuestas al punto.\n" +
                        "\n" +
                        "[EMOCIÓN DETECTADA]: " + emotion + "\n" +
                        "\n" +
                        "[OBLIGATORIO]:\n" +
                        "- Al generar la respuesta, ten en cuenta la emoción detectada en el mensaje del cliente.\n" +
                        "- Adapta el tono y estilo de comunicación según la emoción identificada para brindar una respuesta más empática y acorde al estado emocional del cliente.\n" +
                        "\n" +
                        "[TICKETS DEL USUARIO]:\n" +
                        ticketsInfo.toString()
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
                        "[OBLIGATORIO] Responde 'Sí' si la frase indica resolución o 'No' si la frase no indica resolución.\n\n" +
                        "[FRASE]: " + userMessage
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
                "[INSTRUCCIONES]: Evalúa si el siguiente mensaje del cliente requiere la creación de un nuevo ticket de soporte." +
                        "[OBLIGATORIO] Responde 'Sí' si el mensaje indica la necesidad de crear un nuevo ticket o 'No' si no se requiere, incluyendo situaciones donde el cliente se refiere a tickets ya existentes.\n\n" +
                        "[MENSAJE]: " + userMessage +
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
        System.out.println(assistantMessage.getContent().trim().equalsIgnoreCase("Sí"));
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

}
