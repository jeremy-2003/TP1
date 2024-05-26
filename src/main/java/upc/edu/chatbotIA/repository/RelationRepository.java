package upc.edu.chatbotIA.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import upc.edu.chatbotIA.model.Relation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface RelationRepository extends CrudRepository<Relation, Long> {
    Optional<Relation> findByUserNumber(String userId);
    List<Relation> findByLastInteractionTimeBefore(LocalDateTime threshold);
    @Modifying
    @Query("UPDATE Relation SET company = :company WHERE user_number = :userNumber")
    void updateCompanyName(@Param("userNumber") String userNumber, @Param("company") String company);
}
