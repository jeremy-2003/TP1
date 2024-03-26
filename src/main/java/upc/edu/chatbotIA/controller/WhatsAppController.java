package upc.edu.chatbotIA.controller;

import com.infobip.ApiException;
import com.infobip.api.WhatsAppApi;
import com.infobip.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import upc.edu.chatbotIA.service.WhatsAppService;

@RestController
@RequestMapping("/whatsapp")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    public WhatsAppController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

/*
    {
      "to": "51935101723",
      "content": {
        "text": "Hola, este es un mensaje de prueba enviado desde Spring Boot."
      }
    }
*/
    @PostMapping("/send-message")
    public String sendMessage(@RequestBody WhatsAppTextMessage requestBody) {
        try {
            String to = requestBody.getTo();
            String message = requestBody.getContent().getText();
            whatsAppService.sendTextMessage(to, message);
            return "Mensaje enviado correctamente";
        } catch (Exception e) {
            return "Error al enviar el mensaje: " + e.getMessage();
        }
    }

    @PostMapping("/send-audio")
    public String sendAudioMessage(@RequestBody WhatsAppAudioMessage requestBody) {
        try {
            String to = requestBody.getTo();
            String audioUrl = requestBody.getContent().getMediaUrl();
            whatsAppService.sendAudioMessage(to, audioUrl);
            return "Mensaje de audio enviado correctamente";
        } catch (Exception e) {
            return "Error al enviar el mensaje de audio: " + e.getMessage();
        }
    }

}