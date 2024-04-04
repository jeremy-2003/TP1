package upc.edu.chatbotIA.model;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("relation")
@Getter
@Setter
public class Relation {
    @Id
    private Long id;
    private String userId;
    private Integer dni;
    private String name;
}
