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

        // Construir la lista de mensajes de chat incluyendo el historial de conversación
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (Conversation conversation : previousConversations) {
            chatMessages.add(new ChatMessage("user", conversation.getPrompt()));
            chatMessages.add(new ChatMessage("assistant", conversation.getResponse()));
        }
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