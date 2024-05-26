package upc.edu.chatbotIA.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import upc.edu.chatbotIA.model.Ticket;

@Repository
public interface TicketRepository extends CrudRepository<Ticket, Long> {

}