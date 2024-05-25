package upc.edu.chatbotIA.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.ChatInteractionMetrics;
import upc.edu.chatbotIA.repository.ChatInteractionMetricsRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ChatInteractionMetricsService {
    private final ChatInteractionMetricsRepository chatInteractionMetricsRepository;

    @Autowired
    public ChatInteractionMetricsService(ChatInteractionMetricsRepository chatInteractionMetricsRepository) {
        this.chatInteractionMetricsRepository = chatInteractionMetricsRepository;
    }

    public ChatInteractionMetrics startInteraction(String userNumber) {
        ChatInteractionMetrics interaction = new ChatInteractionMetrics();
        interaction.setUserNumber(userNumber);
        interaction.setStartTime(LocalDateTime.now());
        return chatInteractionMetricsRepository.save(interaction);
    }

    public void endInteraction(ChatInteractionMetrics interaction) {
        interaction.setEndTime(LocalDateTime.now());
        chatInteractionMetricsRepository.save(interaction);
    }

    public Optional<ChatInteractionMetrics> findActiveInteraction(String userNumber) {
        return chatInteractionMetricsRepository.findByUserNumberAndEndTimeIsNull(userNumber);
    }

    public void markInteractionAsAbandoned(ChatInteractionMetrics interaction) {
        interaction.setAbandoned(true);
        chatInteractionMetricsRepository.save(interaction);
    }

    public void markInteractionAsRequestedAdvisor(ChatInteractionMetrics interaction) {
        interaction.setRequestedAdvisor(true);
        chatInteractionMetricsRepository.save(interaction);
    }

    public void updateAverageResponseTime(ChatInteractionMetrics interaction, long responseTime) {
        double currentAverage = interaction.getAverageResponseTime();
        int currentCount = interaction.getMessageCount();

        double newAverage = (currentAverage * currentCount + responseTime) / (currentCount + 1);
        interaction.setAverageResponseTime(newAverage);
        interaction.setMessageCount(currentCount + 1);

        chatInteractionMetricsRepository.save(interaction);
    }

    public void markInteractionAsResolvedInFirstContact(ChatInteractionMetrics interaction) {
        interaction.setResolvedInFirstContact(true);
        chatInteractionMetricsRepository.save(interaction);
    }
}