package upc.edu.chatbotIA.model;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("conversation")
@Getter
@Setter
public class Conversation {

    @Id
    private Long id;

    private String userId;
    private String prompt;
    private String response;
    private LocalDateTime timestamp;

    // Constructor, getters y setters
}
