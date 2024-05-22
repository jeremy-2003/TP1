package upc.edu.chatbotIA.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import upc.edu.chatbotIA.model.RelationAdviserCustomer;

import java.util.List;

public interface RelationAdviserCustomerRepository extends CrudRepository<RelationAdviserCustomer, Long> {
    @Query("SELECT * FROM relation_adviser_customer WHERE user_number = :userNumber AND active = :active")
    RelationAdviserCustomer findByUserNumberAndActive(@Param("userNumber") String userNumber, @Param("active") Boolean active);

    @Query("SELECT * FROM relation_adviser_customer WHERE adviser_number = :adviserNumber AND active = :active")
    RelationAdviserCustomer findByAdviserNumberAndActive(@Param("adviserNumber") String adviserNumber, @Param("active") Boolean active);
    List<RelationAdviserCustomer> findByActive(Boolean active);

}
