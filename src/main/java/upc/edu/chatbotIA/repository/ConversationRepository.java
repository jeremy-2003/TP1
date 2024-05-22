package upc.edu.chatbotIA.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import upc.edu.chatbotIA.model.Conversation;

import java.time.LocalDateTime;
import java.util.List;

public interface ConversationRepository extends CrudRepository<Conversation, Long> {

    @Query("SELECT * FROM conversation WHERE user_number = :userNumber ORDER BY timestamp ASC")
    List<Conversation> findByUserNumberOrderByTimestampAsc(@Param("userNumber") String userNumber);
    @Query("SELECT * FROM Conversation WHERE user_number = :userNumber AND timestamp >= :today AND timestamp < :tomorrow ORDER BY timestamp ASC")
    List<Conversation> findByUserNumberAndTimestampBetweenOrderByTimestampAsc(@Param("userNumber") String userNumber, @Param("today") LocalDateTime today, @Param("tomorrow") LocalDateTime tomorrow);

}
