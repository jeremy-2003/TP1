package upc.edu.chatbotIA.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.Ticket;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TicketService {

    private final SheetsService sheetsService;

    @Autowired
    public TicketService(SheetsService sheetsService) {
        this.sheetsService = sheetsService;
    }

    public Ticket createNewTicket(String senderId, String description, String urgency, String status) {
        String ticketId = generateUniqueId();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        Ticket newTicket = new Ticket(ticketId, senderId, description, urgency, status, createdAt, updatedAt);
        try {
            sheetsService.writeTicketToGoogleSheets(newTicket);
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
        return newTicket;
    }

    public static String generateUniqueId() {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString().replace("-", "");
        String uniqueId = uuidString.substring(0, 8);
        String alphanumericId = "TCK-" + uniqueId;
        return alphanumericId;
    }
}
