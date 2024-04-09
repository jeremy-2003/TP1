package upc.edu.chatbotIA.model;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("relation")
@Getter
@Setter
public class Relation {
    @Id
    private Long id;
    private String userId;
    private Integer dni;
    private String name;
    private LocalDateTime expirationTime;
    private LocalDateTime lastInteractionTime;
    private Boolean active;
}
