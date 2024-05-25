package upc.edu.chatbotIA.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.Adviser;
import upc.edu.chatbotIA.model.RelationAdviserCustomer;
import upc.edu.chatbotIA.repository.AdviserRepository;
import upc.edu.chatbotIA.repository.RelationAdviserCustomerRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RelationAdviserCustomerService {
    private final RelationAdviserCustomerRepository relationAdviserCustomerRepository;
    private final AdviserRepository adviserRepository;

    @Autowired
    public RelationAdviserCustomerService(RelationAdviserCustomerRepository relationAdviserCustomerRepository,
                                         AdviserRepository adviserRepository) {
        this.relationAdviserCustomerRepository = relationAdviserCustomerRepository;
        this.adviserRepository = adviserRepository;
    }

    public Optional<String> buscarAsesorDisponible() {
        // Buscar un asesor disponible
        List<String> asesoresActivos = relationAdviserCustomerRepository.findByActive(true)
                .stream()
                .map(RelationAdviserCustomer::getAdviserNumber)
                .distinct()
                .toList();

        List<String> asesoresRegistrados = adviserRepository.findAllAdviserNumbers()
                .stream()
                .map(Adviser::getAdviserNumber)
                .distinct()
                .toList();

        // Filtrar los asesores disponibles
        List<String> asesoresDisponibles = asesoresRegistrados.stream()
                .filter(numero -> !asesoresActivos.contains(numero))
                .toList();

        // Devolver el primer asesor disponible, si existe
        return asesoresDisponibles.isEmpty() ? Optional.empty() : Optional.of(asesoresDisponibles.get(0));
    }

    public void guardarRelacion(RelationAdviserCustomer relacionAsesorCustomer) {
        relationAdviserCustomerRepository.save(relacionAsesorCustomer);
    }

    public RelationAdviserCustomer encontrarConversacionesActivas(String userNumber, Boolean statusActive){
        return relationAdviserCustomerRepository.findByUserNumberAndActive(userNumber, statusActive);
    }

    public RelationAdviserCustomer encontrarConversacionesActivasAdviserNumber(String adviserNumber, Boolean statusActive){
        return relationAdviserCustomerRepository.findByAdviserNumberAndActive(adviserNumber, statusActive);
    }

    public void finalizarConversacion(RelationAdviserCustomer relacionAsesorCliente) {
        relacionAsesorCliente.setActive(false);
        relacionAsesorCliente.setLastTimeInteraction(LocalDateTime.now());
        relationAdviserCustomerRepository.save(relacionAsesorCliente);
    }
}
