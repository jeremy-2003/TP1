package upc.edu.chatbotIA.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
@Table("blocked_user")
@Getter
@Setter
public class BlockedUser {
    @Id
    private Long id;
    private String userId;
    private LocalDateTime blockTime;
}
