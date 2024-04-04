package upc.edu.chatbotIA.repository;

import org.springframework.data.repository.CrudRepository;
import upc.edu.chatbotIA.model.Relation;

import java.util.Optional;

public interface RelationRepository extends CrudRepository<Relation, Long> {
    Optional<Relation> findByUserId(String userId);

}
