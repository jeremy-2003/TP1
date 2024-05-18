package upc.edu.chatbotIA.model;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("emotions_dictionary")
@Getter
@Setter
public class EmotionsDictionary {
    @Id
    private Long id;
    private String emotion;
    private String wordRelation;
}
