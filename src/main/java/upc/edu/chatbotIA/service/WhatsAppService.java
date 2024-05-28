package upc.edu.chatbotIA.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobip.ApiClient;
import com.infobip.ApiException;
import com.infobip.api.WhatsAppApi;
import com.infobip.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public void sendListMessage(String to, String bodyText, List<Map<String, String>> rows) {
        // Crear el mensaje interactivo de lista
        WhatsAppInteractiveListMessage message = new WhatsAppInteractiveListMessage();
        message.setFrom(senderName);
        message.setTo(to);

        // Crear el contenido del cuerpo del mensaje
        WhatsAppInteractiveBodyContent bodyContent = new WhatsAppInteractiveBodyContent();
        bodyContent.setText(bodyText);

        // Crear la acción de la lista
        WhatsAppInteractiveListActionContent actionContent = new WhatsAppInteractiveListActionContent();
        actionContent.setTitle("Opciones"); // Título ajustado para cumplir con el requisito de longitud

        // Crear las secciones y agregar las filas
        List<WhatsAppInteractiveListSectionContent> sections = new ArrayList<>();
        WhatsAppInteractiveListSectionContent section = new WhatsAppInteractiveListSectionContent();
        section.setTitle("Días disponibles");

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

    public void sendButtonMessage(String recipientNumber, String messageText, List<Map<String, String>> buttons) {
        WhatsAppInteractiveButtonsMessage message = new WhatsAppInteractiveButtonsMessage();
        message.setFrom(senderName);
        message.setTo(recipientNumber);

        WhatsAppInteractiveBodyContent bodyContent = new WhatsAppInteractiveBodyContent();
        bodyContent.setText(messageText);

        List<WhatsAppInteractiveButtonContent> actionButtons = buttons.stream()
                .map(button -> new WhatsAppInteractiveReplyButtonContent()
                        .id(button.get("id"))
                        .title(button.get("title")))
                .collect(Collectors.toList());

        WhatsAppInteractiveButtonsActionContent actionContent = new WhatsAppInteractiveButtonsActionContent();
        actionContent.setButtons(actionButtons);

        // Crear el contenido completo del mensaje interactivo
        WhatsAppInteractiveButtonsContent content = new WhatsAppInteractiveButtonsContent();
        content.setBody(bodyContent);
        content.setAction(actionContent);

        message.setContent(content);

        try {
            WhatsAppSingleMessageInfo response = whatsAppApi.sendWhatsAppInteractiveButtonsMessage(message).execute();
            System.out.println("Respuesta de la API: " + response.getStatus().getDescription());
        } catch (ApiException e) {
            System.err.println("Error al enviar el mensaje interactivo de botones: " + e.getMessage());
            e.printStackTrace();
            System.err.println("Cuerpo de la respuesta de error: " + e.rawResponseBody());
        }
    }
}