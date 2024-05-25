package upc.edu.chatbotIA.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import upc.edu.chatbotIA.model.ChatInteractionMetrics;

import java.util.Optional;

@Repository
public interface ChatInteractionMetricsRepository extends CrudRepository<ChatInteractionMetrics, Long> {
    Optional<ChatInteractionMetrics> findByUserNumberAndEndTimeIsNull(String userNumber);

    @Query("SELECT COUNT(*) FROM chat_interaction_metrics")
    long getTotalInteractions();

    @Query("SELECT COUNT(*) FROM chat_interaction_metrics WHERE abandoned = true")
    long getAbandonedInteractions();

    @Query("SELECT COUNT(*) FROM chat_interaction_metrics WHERE requested_advisor = true")
    long getAdvisorRequestedInteractions();

    @Query("SELECT COUNT(*) FROM chat_interaction_metrics WHERE resolved_in_first_contact = true")
    long getResolvedInFirstContactCount();

    @Query("SELECT AVG(average_response_time) FROM chat_interaction_metrics")
    double getAverageResponseTime();

}