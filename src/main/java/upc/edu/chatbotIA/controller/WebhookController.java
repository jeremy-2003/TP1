package upc.edu.chatbotIA.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobip.model.*;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import upc.edu.chatbotIA.service.ChatGptService;
import upc.edu.chatbotIA.service.TranscriptionService;
import upc.edu.chatbotIA.service.WhatsAppService;
import upc.edu.chatbotIA.util.AudioDownloader;


import java.io.File;
import java.io.IOException;

@RestController
public class WebhookController {

    private final ObjectMapper objectMapper;
    private final AudioDownloader audioDownloader;
    private final TranscriptionService transcriptionService;
    private final ChatGptService chatGptService;
    private final WhatsAppService whatsAppService;

    @Autowired
    public WebhookController(ObjectMapper objectMapper, AudioDownloader audioDownloader, TranscriptionService transcriptionService, ChatGptService chatGptService, WhatsAppService whatsAppService) {
        this.objectMapper = objectMapper;
        this.audioDownloader = audioDownloader;
        this.transcriptionService = transcriptionService;
        this.chatGptService = chatGptService;
        this.whatsAppService = whatsAppService;
    }

    @PostMapping("/incoming-whatsapp")
    public ResponseEntity<Void> receiveWhatsApp(@RequestBody String requestBody) {
        try {
            WhatsAppWebhookInboundMessageResult messages = objectMapper.readValue(requestBody, WhatsAppWebhookInboundMessageResult.class);
            for (WhatsAppWebhookInboundMessageData messageData : messages.getResults()) {
                WhatsAppWebhookInboundMessage message = messageData.getMessage();
                String messageType = String.valueOf(message.getType());
                if (messageType.equals("TEXT")) {
                    WhatsAppWebhookInboundTextMessage textMessage = (WhatsAppWebhookInboundTextMessage) message;
                    String text = textMessage.getText();
                    ChatMessage chatMessage = chatGptService.getChatCompletion(messageData.getFrom(), text);
                    String responseText = chatMessage.getContent();
                    whatsAppService.sendTextMessage(messageData.getFrom(), responseText);
                } else if (messageType.equals("VOICE")) {
                    WhatsAppWebhookInboundVoiceMessage voiceMessage = (WhatsAppWebhookInboundVoiceMessage) message;
                    String voiceUrl = voiceMessage.getUrl();
                    System.out.println("Voice message received:");
                    System.out.println("URL: " + voiceUrl);
                    // Descargar el audio desde la URL y guardar con extensión MP3
                    File mp3File = audioDownloader.downloadAudio(voiceUrl);
                    // Transcribir el audio a texto
                    String transcription = transcriptionService.transcribeAudio(mp3File);
                    System.out.println("Transcription: " + transcription);
                    chatGptService.getChatCompletion(messageData.getFrom(), transcription);
                } else {
                    System.out.println("Unsupported message type: " + messageType);
                }
            }
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}