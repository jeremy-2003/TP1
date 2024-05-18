package upc.edu.chatbotIA.service;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.gson.JsonObject;
import upc.edu.chatbotIA.model.EmotionsDictionary;

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
    private final EmotionsDictionaryService emotionsDictionaryService;

    public SentimentAnalysisService(OpenAiService openAiService, EmotionsDictionaryService emotionsDictionaryService) {
        this.openAiService = openAiService;
        this.emotionsDictionaryService = emotionsDictionaryService;
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

        // Obtener las emociones y palabras relacionadas de la base de datos
        List<EmotionsDictionary> emotionsDictionaries = emotionsDictionaryService.findAll();
        StringBuilder emotionsDictionaryPrompt = new StringBuilder();
        for (EmotionsDictionary emotionsDictionary : emotionsDictionaries) {
            emotionsDictionaryPrompt.append("Emoción: ")
                    .append(emotionsDictionary.getEmotion())
                    .append(", Palabras relacionadas: ")
                    .append(emotionsDictionary.getWordRelation())
                    .append("\n");
        }

        String prompt = "Por favor, analiza el siguiente mensaje y determina la emoción predominante, así como cualquier palabra o frase relacionada." +
                "\n\n[OBLIGATORIO]" +
                "\n- Evita dar como emoción 'Negativo' o 'Positivo'. En su lugar, proporciona emociones más específicas y descriptivas." +
                "\n- Las palabras o frases relacionadas deben estar presentes en el mensaje original." +
                "\n\n[INSTRUCCIONES]" +
                "\n- Utiliza el diccionario de emociones registrado como guía para ayudarte a relacionar y escoger una emoción de forma más precisa." +
                "\n- No es necesario limitarte únicamente a las emociones presentes en el diccionario. Puedes considerar otras emociones relevantes según el análisis del mensaje." +
                "\n- Asegúrate de que las palabras o frases relacionadas estén directamente vinculadas a la emoción predominante identificada." +
                "\n\nMensaje: \"" + message + "\"" +
                "\n\nAnálisis previo: " + analyzeText +
                "\n\nDiccionario de emociones registrado:\n" + emotionsDictionaryPrompt.toString() +
                "\n\nDebes devolver un JSON con dos campos: 'emocion_predominante' y 'palabras_relacionadas'." +
                "\n- El campo 'emocion_predominante' debe representar la emoción principal detectada en el mensaje." +
                "\n- El campo 'palabras_relacionadas' debe contener una lista de palabras o frases del mensaje original que estén relacionadas con la emoción predominante identificada.";

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