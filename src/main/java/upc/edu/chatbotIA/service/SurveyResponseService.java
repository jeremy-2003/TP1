package upc.edu.chatbotIA.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.SurveyResponse;
import upc.edu.chatbotIA.repository.SurveyResponseRepository;

@Service
public class SurveyResponseService {
    private final SurveyResponseRepository surveyResponseRepository;

    @Autowired
    public SurveyResponseService(SurveyResponseRepository surveyResponseRepository) {
        this.surveyResponseRepository = surveyResponseRepository;
    }

    public void saveSurveyResponse(SurveyResponse surveyResponse) {
        surveyResponseRepository.save(surveyResponse);
    }
}