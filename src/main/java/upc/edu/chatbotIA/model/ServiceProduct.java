package upc.edu.chatbotIA.model;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor
public class ServiceProduct {
    private String ruc;
    private String nombre;
    private String estado;
    private LocalDateTime fechaPago;
    private String velocidad;
    private String precio;
}
