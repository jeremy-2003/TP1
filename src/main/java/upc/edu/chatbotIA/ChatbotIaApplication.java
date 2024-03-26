package upc.edu.chatbotIA;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import upc.edu.chatbotIA.config.InfobipApiClient;

import java.io.IOException;

@SpringBootApplication
public class ChatbotIaApplication {
	// Variable global para almacenar el token
	public static String infobipToken;

	// Inyección de las propiedades desde application.properties
	@Value("${infobip.username}")
	private String username;

	@Value("${infobip.password}")
	private String password;

	public static void main(String[] args) {
		SpringApplication.run(ChatbotIaApplication.class, args);
	}

	@PostConstruct
	public void init() {
		// Crear el cliente InfobipApiClient con las credenciales cargadas desde application.properties
		InfobipApiClient client = new InfobipApiClient();
		try {
			String token = client.getSessionToken(username, password);
			System.out.println("Token: " + token);

			// Extraer el valor del token del objeto JSON
			String extractedToken = extractToken(token);

			// Almacenar el token extraído en la variable global
			ChatbotIaApplication.infobipToken = extractedToken;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Método para extraer el valor del token del objeto JSON
	private String extractToken(String jsonResponse) {
		// Remueve los caracteres innecesarios del JSON
		jsonResponse = jsonResponse.replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("\"", "");
		// Separa el JSON en pares clave-valor
		String[] keyValuePairs = jsonResponse.split(",");
		// Busca el par que contiene el token
		for (String pair : keyValuePairs) {
			String[] entry = pair.split(":");
			// Si la clave es "token", devuelve el valor
			if (entry[0].trim().equals("token")) {
				return entry[1].trim();
			}
		}
		// Si no se encuentra el token, devuelve una cadena vacía
		return "";
	}
}
