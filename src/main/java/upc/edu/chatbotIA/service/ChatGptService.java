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

        ChatMessage systemMessage = new ChatMessage();

        // Construir la lista de mensajes de chat incluyendo el historial de conversación
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (Conversation conversation : previousConversations) {
            chatMessages.add(new ChatMessage("user", conversation.getPrompt()));
            chatMessages.add(new ChatMessage("assistant", conversation.getResponse()));
        }

        systemMessage.setRole("system");
        systemMessage.setContent("[INSTRUCCIONES]: Actúa como un chatbot llamado 'TeleBuddy' el cual te encargarás de la atención al cliente para la empresa TelecomunicacionesCenter." +
                "\n[INSTRUCCIONES]: Solo tendrás que responder lo que está en la base de datos. Este será tu base de datos: " +
                "Tienes que brindar información de los servicios de cable e internet. TelecomunicacionesCenter ofrece distintos planes de fibra óptica, DSL, y en cable es solo satelital con 100 canales entre extranjeros y nacionales." +
                " Nos puedes contactar por WhatsApp mediante el número 994 283 802." +
                "\n[IMPORTANTE]: Deberás responder las preguntas sin extenderte mucho, con el fin de tener una conversación fluida " +
                "donde deberás interactuar con el cliente con el fin de brindar información y solucionar dudas." +
                "\n[EMOCIÓN DETECTADA]: " + emotion +
                "\n[INSTRUCCIONES ADICIONALES]: Al generar tu respuesta, ten en cuenta la emoción detectada en el mensaje del cliente. Adapta tu tono y estilo de comunicación según la emoción identificada para brindar una respuesta más empática y acorde a su estado emocional.");

        chatMessages.add(systemMessage);
        chatMessages.add(new ChatMessage("user", userMessage));

        // Construir la solicitud de completado de chat
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(chatMessages)
                .model("gpt-3.5-turbo")
                .build();

        // Enviar la solicitud al servicio de OpenAI
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
}