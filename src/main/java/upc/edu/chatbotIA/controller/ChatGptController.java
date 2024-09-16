package upc.edu.chatbotIA.controller;

import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import upc.edu.chatbotIA.dto.ChatRequest;
import upc.edu.chatbotIA.dto.ChatResponse;
import upc.edu.chatbotIA.service.ChatGptService;

@RestController
@RequestMapping("/openai")
public class ChatGptController {

    private final ChatGptService chatGptService;

    @Autowired
    public ChatGptController(ChatGptService chatGptService) {
        this.chatGptService = chatGptService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestHeader("userId") String userId,
            @RequestBody ChatRequest chatRequest
    ) {
        String userMessage = chatRequest.getMessage();
        ChatMessage assistantMessage = chatGptService.getChatCompletion(userId, userMessage, "");

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setMessage(assistantMessage.getContent());

        return ResponseEntity.ok(chatResponse);
    }
}