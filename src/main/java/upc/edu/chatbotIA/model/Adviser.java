package upc.edu.chatbotIA.model;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
@Table("adviser")
@Getter
@Setter
public class Adviser {
    @Id
    private Long id;
    private String adviserNumber;
    private String name;
}
