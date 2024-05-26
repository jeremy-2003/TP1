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
    private String userNumber;
    private Long userId;
    private String name;
    private String ruc;
    private String company;
    private LocalDateTime expirationTime;
    private LocalDateTime lastInteractionTime;
    private Boolean active;
    private Boolean client;
}
