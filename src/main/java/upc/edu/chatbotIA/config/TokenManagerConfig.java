package upc.edu.chatbotIA.config;

import org.springframework.stereotype.Component;
import upc.edu.chatbotIA.config.InfobipApiClient;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class TokenManagerConfig {

    private String infobipToken;
    private LocalDateTime tokenExpiration;

    public String getInfobipToken(String username, String password) {
        if (infobipToken == null || tokenExpiration == null || LocalDateTime.now().isAfter(tokenExpiration)) {
            refreshToken(username, password);
        }
        return infobipToken;
    }

    public void refreshToken(String username, String password) {
        InfobipApiClient client = new InfobipApiClient();
        try {
            String token = client.getSessionToken(username, password);
            infobipToken = extractToken(token);
            tokenExpiration = LocalDateTime.now().plusHours(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractToken(String jsonResponse) {
        jsonResponse = jsonResponse.replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("\"", "");
        String[] keyValuePairs = jsonResponse.split(",");
        for (String pair : keyValuePairs) {
            String[] entry = pair.split(":");
            if (entry[0].trim().equals("token")) {
                return entry[1].trim();
            }
        }
        return "";
    }
}
