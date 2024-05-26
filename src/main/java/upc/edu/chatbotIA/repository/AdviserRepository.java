package upc.edu.chatbotIA.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import upc.edu.chatbotIA.model.Adviser;

import java.util.List;

@Repository
public interface AdviserRepository extends CrudRepository<Adviser, String> {
    @Query("SELECT * FROM adviser ORDER BY id ASC ")
    List<Adviser> findAllAdviserNumbers();
}
