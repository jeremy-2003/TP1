package upc.edu.chatbotIA.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor
public class Appointment {
    private String ruc;
    private String service;
    private LocalDateTime visitDate;
    private String observation;
}
