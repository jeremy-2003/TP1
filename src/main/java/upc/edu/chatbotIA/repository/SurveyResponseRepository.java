package upc.edu.chatbotIA.repository;

import org.springframework.data.repository.CrudRepository;
import upc.edu.chatbotIA.model.SurveyResponse;

import java.util.List;
import java.util.Optional;

public interface SurveyResponseRepository extends CrudRepository<SurveyResponse, Long> {
    List<SurveyResponse> findByCustomerNumberAndQuestionNumber(String customerNumber, int questionNumber);
}
