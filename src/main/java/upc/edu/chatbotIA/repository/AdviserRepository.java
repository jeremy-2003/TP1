package upc.edu.chatbotIA.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import upc.edu.chatbotIA.model.Adviser;

import java.util.List;

public interface AdviserRepository extends CrudRepository<Adviser, String> {
    @Query("SELECT * FROM adviser ORDER BY id ASC ")
    List<Adviser> findAllAdviserNumbers();
}
