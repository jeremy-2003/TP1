package upc.edu.chatbotIA.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor
public class Ticket {
    private String id;
    private String senderId;
    private String description;
    private String urgency;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
