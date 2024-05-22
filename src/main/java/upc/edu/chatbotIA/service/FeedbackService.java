package upc.edu.chatbotIA.service;

import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.Feedback;
import upc.edu.chatbotIA.repository.FeedbackRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public void saveFeedback(String senderId, int rating, String comment) {
        Feedback feedback = new Feedback();
        feedback.setSenderId(senderId);
        feedback.setRating(rating);
        feedback.setComment(comment);
        feedback.setCreatedAt(LocalDateTime.now());
        feedbackRepository.save(feedback);
    }

    public Optional<Feedback> getFeedbackBySenderId(String senderId) {
        return feedbackRepository.findBySenderId(senderId);
    }
}