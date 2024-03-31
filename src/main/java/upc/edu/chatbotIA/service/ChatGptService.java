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

    public ChatMessage getChatCompletion(String userId, String userMessage) {
        // Recuperar las conversaciones anteriores del usuario
        List<Conversation> previousConversations = conversationRepository.findByUserIdOrderByTimestampAsc(userId);
        ChatMessage systemMessage = new ChatMessage();
        // Construir la lista de mensajes de chat incluyendo el historial de conversación
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (Conversation conversation : previousConversations) {
            chatMessages.add(new ChatMessage("user", conversation.getPrompt()));
            chatMessages.add(new ChatMessage("assistant", conversation.getResponse()));
        }
        systemMessage.setRole("system");
        systemMessage.setContent("[INSTRUCCIONES]: Actua como un chatbot llamado 'TeleBuddy' el cual te encargaras de la atencion al cliente para la empresa TelecomunicacionesCenter" +
                "[INSTRUCCIONES]: Solo tendras que responder lo que esta en la base de datos, Este sera tu base de datos: "+
                " Tienes que brindar información de los servicios de cable y internet. TelecomunicacionesCenter ofrece distintos planes de fibra optica, DSL, y en cable es solo satelital con 100 canales entre extranjeron y nacionales." +
                " Nos puedes contactar por whatsapp mediante el numero 994 283 802 }\n" +
                "[IMPORTANTE]: Deberas de responder las preguntas sin extenderte mucho, con el fin de tener una conversacion fluida" +
                "donde debera interactuar con el cliente con el fin de que brindar información y solucionar dudas");

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
        conversation.setUserId(userId);
        conversation.setPrompt(userMessage);
        conversation.setResponse(assistantMessage.getContent());
        conversation.setTimestamp(LocalDateTime.now());
        conversationRepository.save(conversation);

        return assistantMessage;
    }
}