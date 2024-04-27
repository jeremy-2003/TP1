package upc.edu.chatbotIA.model;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(" ")
@Getter
@Setter
public class RelationAdviserCustomer {
    @Id
    private Long id;
    private String adviserNumber;
    private String userNumber;
    private LocalDateTime firstTimeInteraction;
    private LocalDateTime lastTimeInteraction;
    private boolean active;
}
