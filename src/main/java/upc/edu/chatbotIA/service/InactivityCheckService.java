package upc.edu.chatbotIA.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.ChatInteractionMetrics;
import upc.edu.chatbotIA.model.Relation;
import upc.edu.chatbotIA.model.RelationAdviserCustomer;
import upc.edu.chatbotIA.repository.RelationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InactivityCheckService {
    private final RelationRepository relationRepository;
    private final WhatsAppService whatsAppService;
    private final RelationAdviserCustomerService relationAdviserCustomerService;
    private final ChatInteractionMetricsService chatInteractionMetricsService;

    public InactivityCheckService(RelationRepository relationRepository, WhatsAppService whatsAppService, RelationAdviserCustomerService relationAdviserCustomerService,
                                  ChatInteractionMetricsService chatInteractionMetricsService) {
        this.relationRepository = relationRepository;
        this.whatsAppService = whatsAppService;
        this.relationAdviserCustomerService = relationAdviserCustomerService;
        this.chatInteractionMetricsService = chatInteractionMetricsService;
    }

    @Scheduled(fixedRate = 60000)
    public void checkInactiveConversations() {
        LocalDateTime inactivityThreshold = LocalDateTime.now().minusMinutes(30);
        List<Relation> inactiveRelations = relationRepository.findByLastInteractionTimeBefore(inactivityThreshold);

        for (Relation relation : inactiveRelations) {
            if (relation.getActive()) {
                String senderId = relation.getUserNumber();
                RelationAdviserCustomer relacionAsesorCliente = relationAdviserCustomerService.encontrarConversacionesActivas(senderId, true);
                Optional<ChatInteractionMetrics> interactionOpt = chatInteractionMetricsService.findActiveInteraction(senderId);

                if (relacionAsesorCliente == null && interactionOpt.isPresent()) {
                    ChatInteractionMetrics interaction = interactionOpt.get();

                    if (interaction.getEndTime() == null) {
                        chatInteractionMetricsService.markInteractionAsAbandoned(interaction);
                        chatInteractionMetricsService.endInteraction(interaction);
                        sendInactivityMessage(senderId);
                    }
                }
                relation.setActive(false);
                relationRepository.save(relation);
            }
        }
    }

    private void sendInactivityMessage(String senderId) {
        String inactivityMessage = "🔔 La conversación se ha cerrado debido a la falta de interacción durante los últimos 30 minutos. Si necesitas ayuda adicional, no dudes en contactarnos nuevamente. ¡Estamos aquí para ayudarte!";
        whatsAppService.sendTextMessage(senderId, inactivityMessage);
    }
}