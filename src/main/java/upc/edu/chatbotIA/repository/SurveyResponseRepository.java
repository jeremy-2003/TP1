package upc.edu.chatbotIA.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import upc.edu.chatbotIA.model.SurveyResponse;

import java.util.List;

@Repository
public interface SurveyResponseRepository extends CrudRepository<SurveyResponse, Long> {
    List<SurveyResponse> findByCustomerNumberAndQuestionNumber(String customerNumber, int questionNumber);
}
