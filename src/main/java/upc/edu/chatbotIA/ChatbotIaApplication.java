package upc.edu.chatbotIA;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import upc.edu.chatbotIA.config.InfobipApiClient;
import upc.edu.chatbotIA.config.TokenManagerConfig;

import java.io.IOException;

@SpringBootApplication
public class ChatbotIaApplication {
	@Autowired
	private TokenManagerConfig tokenManager;
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
		tokenManager.refreshToken(username, password);

	}


}
