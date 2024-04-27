package upc.edu.chatbotIA.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import upc.edu.chatbotIA.model.BlockedUser;

@Repository
public interface BlockedUserRepository extends CrudRepository<BlockedUser, String> {
    BlockedUser findByUserNumber(String userNumber);
}