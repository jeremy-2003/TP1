package upc.edu.chatbotIA;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import upc.edu.chatbotIA.config.TokenManagerConfig;

@EnableScheduling
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
