package upc.edu.chatbotIA.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.Relation;
import upc.edu.chatbotIA.repository.RelationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InactivityCheckService {
    private final RelationRepository relationRepository;
    private final WhatsAppService whatsAppService;

    public InactivityCheckService(RelationRepository relationRepository, WhatsAppService whatsAppService) {
        this.relationRepository = relationRepository;
        this.whatsAppService = whatsAppService;
    }

    @Scheduled(fixedRate = 60000)
    public void checkInactiveConversations() {
        LocalDateTime inactivityThreshold = LocalDateTime.now().minusMinutes(2);
        List<Relation> inactiveRelations = relationRepository.findByLastInteractionTimeBefore(inactivityThreshold);

        for (Relation relation : inactiveRelations) {
            if (relation.getActive()) {
                sendInactivityMessage(relation.getUserNumber());
                relation.setActive(false);
                relationRepository.save(relation);
            }
        }
    }

    private void sendInactivityMessage(String senderId) {
        String inactivityMessage = "La conversación se ha cerrado debido a la falta de interacción durante los últimos 10 minutos. Si necesitas ayuda adicional, no dudes en contactarnos nuevamente.";
        whatsAppService.sendTextMessage(senderId, inactivityMessage);
    }
}