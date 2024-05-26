package upc.edu.chatbotIA.service;

import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.ServiceProduct;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
public class ServiceProductService {
    private final SheetsService sheetsService;

    public ServiceProductService(SheetsService sheetsService) {
        this.sheetsService = sheetsService;
    }

    public List<ServiceProduct> getServiceProductsByRucAndEstado(String ruc) {
        try {
            return sheetsService.getServiciosByRucAndEstado(ruc);
        } catch (IOException | GeneralSecurityException e) {
            // Manejar las excepciones adecuadamente (registrar, lanzar una excepción personalizada, etc.)
            throw new RuntimeException("Error al obtener los servicios por RUC y estado", e);
        }
    }
}
