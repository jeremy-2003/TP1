package upc.edu.chatbotIA.config;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {
    @Value("${openai.api.token}")
    private String apiKey;
    @Bean
    public OpenAiService openAiService() {
        return new OpenAiService(apiKey);
    }
}