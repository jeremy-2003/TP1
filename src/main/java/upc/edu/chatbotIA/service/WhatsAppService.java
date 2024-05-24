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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public void sendInteractiveListMessage(String to, String bodyText, List<Map<String, String>> rows) {
        // Crear el mensaje interactivo de lista
        WhatsAppInteractiveListMessage message = new WhatsAppInteractiveListMessage();
        message.setFrom(senderName);
        message.setTo(to);

        // Crear el contenido del cuerpo del mensaje
        WhatsAppInteractiveBodyContent bodyContent = new WhatsAppInteractiveBodyContent();
        bodyContent.setText(bodyText);

        // Crear la acción de la lista
        WhatsAppInteractiveListActionContent actionContent = new WhatsAppInteractiveListActionContent();
        actionContent.setTitle("Lista interactiva");

        // Crear las secciones y agregar las filas
        List<WhatsAppInteractiveListSectionContent> sections = new ArrayList<>();
        WhatsAppInteractiveListSectionContent section = new WhatsAppInteractiveListSectionContent();

        List<WhatsAppInteractiveRowContent> sectionRows = new ArrayList<>();
        for (Map<String, String> row : rows) {
            WhatsAppInteractiveRowContent sectionRow = new WhatsAppInteractiveRowContent();
            sectionRow.setId(row.get("id"));
            sectionRow.setTitle(row.get("title"));
            sectionRows.add(sectionRow);
        }

        section.setRows(sectionRows);
        sections.add(section);
        actionContent.setSections(sections);

        // Crear el contenido completo del mensaje interactivo
        WhatsAppInteractiveListContent content = new WhatsAppInteractiveListContent();
        content.setBody(bodyContent);
        content.setAction(actionContent);

        message.setContent(content);

        // Enviar el mensaje a través de la API de Infobip
        try {
            WhatsAppSingleMessageInfo response = whatsAppApi.sendWhatsAppInteractiveListMessage(message).execute();
            System.out.println("Respuesta de la API: " + response.getStatus().getDescription());
        } catch (ApiException e) {
            System.err.println("Error al enviar el mensaje interactivo de lista: " + e.getMessage());
            e.printStackTrace();
            // Imprime el cuerpo de la respuesta de error
            System.err.println("Cuerpo de la respuesta de error: " + e.rawResponseBody());
        }
    }


}