package upc.edu.chatbotIA.controller;

import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import upc.edu.chatbotIA.model.EmotionsDictionary;
import upc.edu.chatbotIA.service.EmotionsDictionaryService;
import upc.edu.chatbotIA.service.SentimentAnalysisService;

import javax.json.*;
import java.io.StringReader;
import java.io.IOException;
import java.io.StringReader;

@RestController("/sentiment")
public class SentimentAnalysisController {

    private final SentimentAnalysisService sentimentAnalysisService;
    private final EmotionsDictionaryService emotionsDictionaryService;

    @Autowired
    public SentimentAnalysisController(SentimentAnalysisService sentimentAnalysisService, EmotionsDictionaryService emotionsDictionaryService) {
        this.sentimentAnalysisService = sentimentAnalysisService;
        this.emotionsDictionaryService = emotionsDictionaryService;
    }
    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeTextWithOpenAi(@RequestBody String message) throws IOException, InterruptedException {
        ChatMessage assistantMessage = sentimentAnalysisService.analyzeTextWithOpenAi(message);
        String contentJsonString = assistantMessage.getContent();
        JsonReader reader = Json.createReader(new StringReader(contentJsonString));
        JsonObject contentJsonObject = reader.readObject();

        String emocionPredominante = contentJsonObject.getString("emocion_predominante");
        JsonArray palabrasRelacionadas = contentJsonObject.getJsonArray("palabras_relacionadas");

        if (palabrasRelacionadas == null) {
            // Handle the case when "palabras_relacionadas" is not present or is not an array
            palabrasRelacionadas = Json.createArrayBuilder().build();
        }

        // Verify if the emotion already exists in the database
        EmotionsDictionary existingEmotionsDictionary = emotionsDictionaryService.findByEmotion(emocionPredominante);

        if (existingEmotionsDictionary != null) {
            // If the emotion already exists, update wordRelation
            String existingWordRelation = existingEmotionsDictionary.getWordRelation();
            String updatedWordRelation = existingWordRelation + ", " + palabrasRelacionadas.toString();
            existingEmotionsDictionary.setWordRelation(updatedWordRelation);
            emotionsDictionaryService.save(existingEmotionsDictionary);
        } else {
            // If the emotion doesn't exist, create a new entry
            EmotionsDictionary newEmotionsDictionary = new EmotionsDictionary();
            newEmotionsDictionary.setEmotion(emocionPredominante);
            newEmotionsDictionary.setWordRelation(palabrasRelacionadas.toString());
            emotionsDictionaryService.save(newEmotionsDictionary);
        }

        JsonObjectBuilder nuevoJsonObjectBuilder = Json.createObjectBuilder();
        nuevoJsonObjectBuilder.add("emocion_predominante", emocionPredominante);
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