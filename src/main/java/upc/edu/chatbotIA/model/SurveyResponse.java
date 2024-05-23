package upc.edu.chatbotIA.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("survey_response")
@Getter
@Setter
public class SurveyResponse {
    @Id
    private Long id; //PK
    private String customerNumber; //Numero de cliente
    private int questionNumber; //numero de pregunta
    private String questionMetric; // que pase vacio por ahora
    private String response; //respuesta a la pregunta
    private LocalDateTime createdAt; //hora de la encuesta
}
