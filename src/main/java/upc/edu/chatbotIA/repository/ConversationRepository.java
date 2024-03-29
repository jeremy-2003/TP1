package upc.edu.chatbotIA.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import upc.edu.chatbotIA.model.Conversation;

import java.util.List;

public interface ConversationRepository extends CrudRepository<Conversation, Long> {

    @Query("SELECT * FROM conversation WHERE user_id = :userId ORDER BY timestamp ASC")
    List<Conversation> findByUserIdOrderByTimestampAsc(@Param("userId") String userId);

    // Puedes agregar más métodos de consulta personalizados aquí si es necesario
}
