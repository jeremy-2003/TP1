package upc.edu.chatbotIA.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("feedback")
@Getter
@Setter
public class Feedback {
    @Id
    private Long id;
    private String senderId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}
