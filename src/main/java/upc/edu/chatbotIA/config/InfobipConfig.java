package upc.edu.chatbotIA.config;


import com.infobip.ApiClient;
import com.infobip.ApiKey;
import com.infobip.BaseUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class InfobipConfig {

    @Value("${infobip.api-key}")
    private String apiKey;

    @Value("${infobip.base-url}")
    private String baseUrl;

    @Bean
    public ApiClient apiClient() {
        return ApiClient.forApiKey(ApiKey.from(apiKey))
                .withBaseUrl(BaseUrl.from(baseUrl))
                .build();
    }
}