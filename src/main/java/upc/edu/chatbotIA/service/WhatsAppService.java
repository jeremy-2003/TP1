package upc.edu.chatbotIA.service;


import com.infobip.ApiClient;
import com.infobip.ApiException;
import com.infobip.api.WhatsAppApi;
import com.infobip.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.print.attribute.standard.Media;

@Service
public class WhatsAppService {
    private final WhatsAppApi whatsAppApi;

    @Value("${infobip.sender}")
    private String senderName;

    public WhatsAppService(ApiClient apiClient) {
        this.whatsAppApi = new WhatsAppApi(apiClient);
    }

    public void sendTextMessage(String to, String message) {
        WhatsAppTextMessage textMessage = new WhatsAppTextMessage()
                .from(senderName)
                .to(to)
                .content(new WhatsAppTextContent()
                        .text(message)
                );

        try {
            WhatsAppSingleMessageInfo messageInfo = whatsAppApi.sendWhatsAppTextMessage(textMessage).execute();
        } catch (ApiException e) {
            System.err.println("Error al enviar el mensaje: " + e.getMessage());
        }
    }

    public void sendAudioMessage(String to, String audioUrl) {
        WhatsAppAudioMessage audioMessage = new WhatsAppAudioMessage()
                .from(senderName)
                .to(to)
                .content(new WhatsAppAudioContent()
                        .mediaUrl(audioUrl)
                );

        try {
            WhatsAppSingleMessageInfo messageInfo = whatsAppApi.sendWhatsAppAudioMessage(audioMessage).execute();
            System.out.println("Respuesta de la API: " + messageInfo.getStatus().getDescription());
        } catch (ApiException e) {
            System.err.println("Error al enviar el mensaje de audio: " + e.getMessage());
        }
    }
}