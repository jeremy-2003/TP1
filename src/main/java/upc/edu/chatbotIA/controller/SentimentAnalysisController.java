package upc.edu.chatbotIA.controller;

import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import upc.edu.chatbotIA.service.SentimentAnalysisService;

import javax.json.*;
import java.io.StringReader;
import java.io.IOException;
import java.io.StringReader;

@RestController("/sentiment")
public class SentimentAnalysisController {

    private final SentimentAnalysisService sentimentAnalysisService;

    @Autowired
    public SentimentAnalysisController(SentimentAnalysisService sentimentAnalysisService) {
        this.sentimentAnalysisService = sentimentAnalysisService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeTextWithOpenAi(@RequestBody String message) throws IOException, InterruptedException {
        ChatMessage assistantMessage = sentimentAnalysisService.analyzeTextWithOpenAi(message);
        String contentJsonString = assistantMessage.getContent();
        JsonReader reader = Json.createReader(new StringReader(contentJsonString));
        JsonObject contentJsonObject = reader.readObject();
        String sentimientoPredominante = contentJsonObject.getString("sentimiento_predominante");
        JsonArray palabrasRelacionadas = contentJsonObject.getJsonArray("palabras_relacionadas");
        JsonObjectBuilder nuevoJsonObjectBuilder = Json.createObjectBuilder();
        nuevoJsonObjectBuilder.add("sentimiento_predominante", sentimientoPredominante);
        JsonArrayBuilder nuevasPalabrasRelacionadasBuilder = Json.createArrayBuilder();
        for (int i = 0; i < palabrasRelacionadas.size(); i++) {
            nuevasPalabrasRelacionadasBuilder.add(Json.createObjectBuilder()
                    .add("value", palabrasRelacionadas.getString(i)));
        }
        nuevoJsonObjectBuilder.add("palabras_relacionadas", nuevasPalabrasRelacionadasBuilder);
        JsonObject nuevoJsonObject = nuevoJsonObjectBuilder.build();
        return new ResponseEntity<>(nuevoJsonObject.toString(), HttpStatus.OK);
    }
}