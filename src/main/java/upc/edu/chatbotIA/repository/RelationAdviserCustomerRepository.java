package upc.edu.chatbotIA.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import upc.edu.chatbotIA.model.RelationAdviserCustomer;

import java.util.List;

public interface RelationAdviserCustomerRepository extends CrudRepository<RelationAdviserCustomer, Long> {
    List<RelationAdviserCustomer> findByAdviserNumberAndActive(String adviserNumber, boolean active);
    List<RelationAdviserCustomer> findByActive(boolean active);

}
