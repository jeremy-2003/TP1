package upc.edu.chatbotIA.repository;

import org.springframework.data.repository.CrudRepository;
import upc.edu.chatbotIA.model.Feedback;

import java.util.Optional;

public interface FeedbackRepository extends CrudRepository<Feedback, Long> {
    Optional<Feedback> findBySenderId(String senderId);
}
