package upc.edu.chatbotIA.service;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class SentimentAnalysisService {
    @Value("${api.sentiment.url}")
    private String sentimentApiUrl;
    private final OpenAiService openAiService;

    public SentimentAnalysisService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public String analyzeText(String text) throws IOException, InterruptedException {
        // Construir el cuerpo JSON de la solicitud
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("text", text);

        // Configurar la solicitud HTTP POST
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(sentimentApiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        // Realizar la llamada HTTP
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Devolver el cuerpo de la respuesta
        return response.body();
    }


    public ChatMessage analyzeTextWithOpenAi(String message) throws IOException, InterruptedException {
        String analyzeText = analyzeText(message);
        String prompt = "Por favor, analiza el siguiente mensaje y determina el sentimiento predominante, así como cualquier palabra o frase relacionada:\n\n" +
                "Mensaje: \"" + message + "\"\n" +
                "Análisis previo: " + analyzeText + "\n\n" +
                "Debes devolver un JSON con dos campos: 'sentimiento_predominante' y 'palabras_relacionadas'. El campo 'sentimiento_predominante' debe representar el sentimiento principal detectado en el mensaje, mientras que 'palabras_relacionadas' debe contener una lista de palabras o frases relacionadas con ese sentimiento, si las hay.";
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage("user", prompt));
        // Construir la solicitud de completado de chat
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(chatMessages)
                .model("gpt-3.5-turbo")
                .build();
        ChatMessage assistantMessage = openAiService.createChatCompletion(completionRequest).getChoices().get(0).getMessage();
        return assistantMessage;
    }

}