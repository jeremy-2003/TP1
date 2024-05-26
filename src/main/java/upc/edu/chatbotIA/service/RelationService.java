package upc.edu.chatbotIA.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.edu.chatbotIA.model.Relation;
import upc.edu.chatbotIA.repository.RelationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RelationService {
    private final RelationRepository relationRepository;

    public RelationService(RelationRepository relationRepository) {
        this.relationRepository = relationRepository;
    }

    @Transactional
    public Optional<Relation> findByUserNumber(String userId) {
        return relationRepository.findByUserNumber(userId);
    }

    @Transactional
    public List<Relation> findByLastInteractionTimeBefore(LocalDateTime threshold) {
        return relationRepository.findByLastInteractionTimeBefore(threshold);
    }

    public Relation save(Relation relation) {
        return relationRepository.save(relation);
    }
    public void delete(Relation relation) {
        relationRepository.delete(relation);
    }

    @Transactional
    public void updateCompanyName(String userNumber, String companyName) {
        relationRepository.updateCompanyName(userNumber, companyName);
    }

    public List<Relation> getAllRelations() {
        return (List<Relation>) relationRepository.findAll();
    }
}