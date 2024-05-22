package upc.edu.chatbotIA.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.Relation;
import upc.edu.chatbotIA.model.RelationAdviserCustomer;
import upc.edu.chatbotIA.repository.RelationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InactivityCheckService {
    private final RelationRepository relationRepository;
    private final WhatsAppService whatsAppService;
    private final RelationAdviserCustomerService relationAdviserCustomerService;

    public InactivityCheckService(RelationRepository relationRepository, WhatsAppService whatsAppService, RelationAdviserCustomerService relationAdviserCustomerService) {
        this.relationRepository = relationRepository;
        this.whatsAppService = whatsAppService;
        this.relationAdviserCustomerService = relationAdviserCustomerService;
    }

    @Scheduled(fixedRate = 60000)
    public void checkInactiveConversations() {
        LocalDateTime inactivityThreshold = LocalDateTime.now().minusMinutes(10);
        List<Relation> inactiveRelations = relationRepository.findByLastInteractionTimeBefore(inactivityThreshold);

        for (Relation relation : inactiveRelations) {
            if (relation.getActive()) {
                String senderId = relation.getUserNumber();
                RelationAdviserCustomer relacionAsesorCliente = relationAdviserCustomerService.encontrarConversacionesActivas(senderId, true);

                if (relacionAsesorCliente == null) {
                    sendInactivityMessage(senderId);
                    relation.setActive(false);
                    relationRepository.save(relation);
                }
            }
        }
    }

    private void sendInactivityMessage(String senderId) {
        String inactivityMessage = "La conversación se ha cerrado debido a la falta de interacción durante los últimos 10 minutos. Si necesitas ayuda adicional, no dudes en contactarnos nuevamente.";
        whatsAppService.sendTextMessage(senderId, inactivityMessage);
    }
}