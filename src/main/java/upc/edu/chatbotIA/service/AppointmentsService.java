package upc.edu.chatbotIA.service;

import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.Appointment;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentsService {
    private final SheetsService sheetsService;

    public AppointmentsService(SheetsService sheetsService) {
        this.sheetsService = sheetsService;
    }

    public List<Appointment> getAppointmentsByRucAndEstado(String ruc) {
        try {
            return sheetsService.getAvailableAppointments(ruc);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Error al obtener las citas por RUC y estado", e);
        }
    }

    public List<LocalDateTime> checkAvailableDates() {
        try {
            return sheetsService.checkAvailableDates();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Error al verificar las fechas disponibles para citas", e);
        }
    }
    public void scheduleAppointment(String ruc, String service, LocalDateTime visitDate, String observation) {
        try {
            sheetsService.scheduleAppointment(ruc, service, visitDate, observation);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Error al agendar la cita", e);
        }
    }
}
