package upc.edu.chatbotIA.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import upc.edu.chatbotIA.service.SentimentAnalysisService;

import java.io.IOException;

@RestController("/sentiment")
public class SentimentAnalysisController {

    private final SentimentAnalysisService sentimentAnalysisService;
    @Autowired
    public SentimentAnalysisController(SentimentAnalysisService sentimentAnalysisService) {
        this.sentimentAnalysisService = sentimentAnalysisService;
    }
    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeTextWithOpenAi(@RequestBody String message) throws IOException, InterruptedException {
        String result = sentimentAnalysisService.analyzeTextAndSaveEmotions(message);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}