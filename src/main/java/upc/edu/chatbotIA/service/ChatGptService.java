package upc.edu.chatbotIA.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.Conversation;
import upc.edu.chatbotIA.repository.ConversationRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatGptService {
    private final OpenAiService openAiService;
    private final ConversationRepository conversationRepository;

    @Autowired
    public ChatGptService(OpenAiService openAiService, ConversationRepository conversationRepository) {
        this.openAiService = openAiService;
        this.conversationRepository = conversationRepository;
    }

    public ChatMessage getChatCompletion(String userId, String userMessage, String emotion) {
        // Recuperar las conversaciones anteriores del usuario
        List<Conversation> previousConversations = conversationRepository.findByUserNumberOrderByTimestampAsc(userId);

        // Crear mensaje del sistema con instrucciones y emoción detectada
        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setRole("system");
        systemMessage.setContent(
                "[INSTRUCCIONES]: Actúa como un chatbot llamado 'TeleBuddy' encargado de la atención al cliente para la empresa TelecomunicacionesCenter.\n" +
                        "[INSTRUCCIONES]: Solo tendrás que responder lo que está en la base de datos. Este será tu base de datos: " +
                        "Tienes que brindar información de los servicios de cable e internet. TelecomunicacionesCenter ofrece distintos planes de fibra óptica, DSL, y en cable es solo satelital con 100 canales entre extranjeros y nacionales. " +
                        "Nos puedes contactar por WhatsApp mediante el número 994 283 802.\n" +
                        "[IMPORTANTE]: Deberás responder las preguntas sin extenderte mucho, con el fin de tener una conversación fluida " +
                        "donde deberás interactuar con el cliente con el fin de brindar información y solucionar dudas.\n" +
                        "[EMOCIÓN DETECTADA]: " + emotion + "\n" +
                        "[INSTRUCCIONES ADICIONALES]: Al generar tu respuesta, ten en cuenta la emoción detectada en el mensaje del cliente. " +
                        "Adapta tu tono y estilo de comunicación según la emoción identificada para brindar una respuesta más empática y acorde a su estado emocional."
        );

        // Construir la lista de mensajes de chat incluyendo el mensaje del sistema y el historial de conversación
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(systemMessage);

        for (Conversation conversation : previousConversations) {
            chatMessages.add(new ChatMessage("user", conversation.getPrompt()));
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
        System.out.println(assistantMessage.getContent().trim().equalsIgnoreCase("Sí"));
        return assistantMessage.getContent().trim().equalsIgnoreCase("Sí");
    }
}
