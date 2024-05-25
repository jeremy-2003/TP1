package upc.edu.chatbotIA.model;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("chat_interaction_metrics")
@Getter
@Setter
public class ChatInteractionMetrics {
    @Id
    private Long id;

    private String userNumber;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean abandoned;
    private boolean requestedAdvisor;
    private boolean resolvedInFirstContact;
    private double averageResponseTime;
    private int messageCount;
}
