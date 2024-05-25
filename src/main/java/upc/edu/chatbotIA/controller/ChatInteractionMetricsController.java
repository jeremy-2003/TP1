package upc.edu.chatbotIA.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import upc.edu.chatbotIA.repository.ChatInteractionMetricsRepository;

@RestController
@RequestMapping("/metrics")
public class ChatInteractionMetricsController {
    private final ChatInteractionMetricsRepository chatInteractionMetricsRepository;

    public ChatInteractionMetricsController(ChatInteractionMetricsRepository chatInteractionMetricsRepository) {
        this.chatInteractionMetricsRepository = chatInteractionMetricsRepository;
    }

    @GetMapping("/total-interactions")
    public ResponseEntity<String> getTotalInteractions() {
        long totalInteractions = chatInteractionMetricsRepository.getTotalInteractions();
        String response = "{\"totalInteractions\": " + totalInteractions + "}";
        return ResponseEntity.ok(response);
    }

    @GetMapping("/abandoned-interactions")
    public ResponseEntity<String> getAbandonedInteractions() {
        long abandonedInteractions = chatInteractionMetricsRepository.getAbandonedInteractions();
        String response = "{\"abandonedInteractions\": " + abandonedInteractions + "}";
        return ResponseEntity.ok(response);
    }

    @GetMapping("/advisor-requested-interactions")
    public ResponseEntity<String> getAdvisorRequestedInteractions() {
        long advisorRequestedInteractions = chatInteractionMetricsRepository.getAdvisorRequestedInteractions();
        String response = "{\"advisorRequestedInteractions\": " + advisorRequestedInteractions + "}";
        return ResponseEntity.ok(response);
    }

    @GetMapping("/resolved-in-first-contact")
    public ResponseEntity<String> getResolvedInFirstContactCount() {
        long resolvedInFirstContactCount = chatInteractionMetricsRepository.getResolvedInFirstContactCount();
        String response = "{\"resolvedInFirstContactCount\": " + resolvedInFirstContactCount + "}";
        return ResponseEntity.ok(response);
    }

    @GetMapping("/average-response-time")
    public ResponseEntity<String> getAverageResponseTime() {
        double averageResponseTime = chatInteractionMetricsRepository.getAverageResponseTime();
        String response = "{\"averageResponseTime\": " + averageResponseTime + "}";
        return ResponseEntity.ok(response);
    }
}