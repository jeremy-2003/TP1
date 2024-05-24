package upc.edu.chatbotIA.service;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.gson.JsonObject;
import upc.edu.chatbotIA.model.EmotionsDictionary;

import javax.json.*;
import java.io.StringReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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


    public String analyzeTextAndSaveEmotions(String message) throws IOException, InterruptedException {
        ChatMessage assistantMessage = analyzeTextWithOpenAi(message);
        String contentJsonString = assistantMessage.getContent().trim(); // Eliminar espacios en blanco al inicio y al final

        // Utilizar expresiones regulares para eliminar las etiquetas "json" al inicio y al final
        contentJsonString = contentJsonString.replaceAll("^```json|```$", "");

        // Procesar el JSON sin las etiquetas "json" al inicio y al final
        JsonReader reader = Json.createReader(new StringReader(contentJsonString));
        javax.json.JsonObject contentJsonObject = reader.readObject();
        String emocionPredominante = contentJsonObject.getString("sentimiento_predominante"); // Cambiado a "sentimiento_predominante"
        JsonArray nuevasPalabrasRelacionadas = contentJsonObject.getJsonArray("palabras_relacionadas");
        if (nuevasPalabrasRelacionadas == null) {
            nuevasPalabrasRelacionadas = Json.createArrayBuilder().build();
        }
        // Guardar las palabras relacionadas como una lista de String
        List<String> wordRelationList = new ArrayList<>();
        for (int i = 0; i < nuevasPalabrasRelacionadas.size(); i++) {
            wordRelationList.add(nuevasPalabrasRelacionadas.getJsonObject(i).getString("value"));
        }

        // Verificar si la emoción ya existe en la base de datos
        EmotionsDictionary existingEmotionsDictionary = emotionsDictionaryService.findByEmotion(emocionPredominante);
        if (existingEmotionsDictionary != null) {
            // Si la emoción ya existe, actualizar wordRelation
            String existingWordRelation = existingEmotionsDictionary.getWordRelation();
            List<String> existingWordRelationList = new ArrayList<>(Arrays.asList(existingWordRelation.replaceAll("\\[|\\]", "").split(", ")));
            existingWordRelationList.addAll(wordRelationList);
            String updatedWordRelation = "[" + String.join(", ", existingWordRelationList) + "]";
            existingEmotionsDictionary.setWordRelation(updatedWordRelation);
            emotionsDictionaryService.save(existingEmotionsDictionary);
        } else {
            // Si la emoción no existe, crear una nueva entrada
            EmotionsDictionary newEmotionsDictionary = new EmotionsDictionary();
            newEmotionsDictionary.setEmotion(emocionPredominante);
            newEmotionsDictionary.setWordRelation("[" + String.join(", ", wordRelationList) + "]");
            emotionsDictionaryService.save(newEmotionsDictionary);
        }
        // Construir el nuevo JSON sin las etiquetas "json" al inicio y al final
        JsonObjectBuilder nuevoJsonObjectBuilder = Json.createObjectBuilder();
        nuevoJsonObjectBuilder.add("sentimiento_predominante", emocionPredominante); // Cambiado a "sentimiento_predominante"
        JsonArrayBuilder nuevasPalabrasRelacionadasBuilder = Json.createArrayBuilder();
        for (int i = 0; i < nuevasPalabrasRelacionadas.size(); i++) {
            nuevasPalabrasRelacionadasBuilder.add(Json.createObjectBuilder()
                    .add("value", nuevasPalabrasRelacionadas.getJsonObject(i).getString("value"))); // Acceder correctamente al valor "value"
        }
        nuevoJsonObjectBuilder.add("palabras_relacionadas", nuevasPalabrasRelacionadasBuilder);
        javax.json.JsonObject nuevoJsonObject = nuevoJsonObjectBuilder.build();
        return nuevoJsonObject.toString();
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

        String prompt = "Por favor, te encargaras de analizar los mensajes enviados por los clientes y determinar la emoción predominante, así como cualquier palabra o frase relacionada." +
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
                "\n\n[OBLIGATORIO]" +
                "\n\nDebes devolver solo un JSON y con dos campos: 'sentimiento_predominante' y 'palabras_relacionadas'." +
                "\n- El campo 'sentimiento_predominante' debe representar el sentimiento principal detectado en el mensaje." +
                "\n- El campo 'palabras_relacionadas' debe contener una lista de objetos donde cada uno tiene un campo 'value', representando una palabra o frase del mensaje original relacionada con el sentimiento predominante identificado.";

        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage("user", prompt));

        // Construir la solicitud de completado de chat
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(chatMessages)
                .model("gpt-4o-2024-05-13")
                .build();

        ChatMessage assistantMessage = openAiService.createChatCompletion(completionRequest).getChoices().get(0).getMessage();
        return assistantMessage;
    }


}