package upc.edu.chatbotIA.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.ChatInteractionMetrics;
import upc.edu.chatbotIA.model.Relation;
import upc.edu.chatbotIA.model.RelationAdviserCustomer;
import upc.edu.chatbotIA.repository.RelationRepository;

import java.time.Duration;
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
        // Define el umbral de inactividad
        LocalDateTime inactivityThreshold = LocalDateTime.now().minusMinutes(30);

        // Obtén las relaciones inactivas
        List<Relation> inactiveRelations = relationRepository.findByLastInteractionTimeBefore(inactivityThreshold);

        // Itera sobre las relaciones inactivas
        for (Relation relation : inactiveRelations) {
            if (relation.getActive()) {
                String senderId = relation.getUserNumber();

                // Verifica si hay interacciones activas en RelationAdviserCustomer
                RelationAdviserCustomer activeConversations = relationAdviserCustomerService.encontrarConversacionesActivas(senderId, true);

                // Obtén la fecha y hora actual
                LocalDateTime currentTime = LocalDateTime.now();

                // Calcula la diferencia entre la fecha y hora actual y el lastTimeInteraction de la relación
                Duration duration = Duration.between(relation.getLastInteractionTime(), currentTime);

                // Verifica si ha pasado más de 30 minutos desde la última interacción
                if (duration.toMinutes() > 30 && activeConversations == null) {
                    // Desactiva la relación
                    relation.setActive(false);
                    relationRepository.save(relation);

                    // Actualiza la interacción como abandonada
                    Optional<ChatInteractionMetrics> activeInteractionOpt = chatInteractionMetricsService.findActiveInteraction(senderId);
                    if (activeInteractionOpt.isPresent()) {
                        ChatInteractionMetrics activeInteraction = activeInteractionOpt.get();
                        activeInteraction.setAbandoned(true);
                        chatInteractionMetricsService.markInteractionAsAbandoned(activeInteraction);
                        chatInteractionMetricsService.endInteraction(activeInteraction);
                    }

                    // Envía un mensaje de inactividad
                    sendInactivityMessage(senderId);
                }
            }
        }
    }


    private void sendInactivityMessage(String senderId) {
        String inactivityMessage = "🔔 La conversación se ha cerrado debido a la falta de interacción durante los últimos 30 minutos. Si necesitas ayuda adicional, no dudes en contactarnos nuevamente. ¡Estamos aquí para ayudarte!";
        whatsAppService.sendTextMessage(senderId, inactivityMessage);
    }
}