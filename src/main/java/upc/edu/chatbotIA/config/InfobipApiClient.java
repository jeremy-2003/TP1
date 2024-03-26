package upc.edu.chatbotIA.config;
import okhttp3.*;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
@Configuration
public class InfobipApiClient {

    private static final String BASE_URL = "https://api.infobip.com";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private OkHttpClient client = new OkHttpClient();

    public String getSessionToken(String username, String password) throws IOException {
        String jsonBody = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        RequestBody requestBody = RequestBody.create(JSON, jsonBody);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/1/session")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseBody = response.body().string();
            // Parse JSON response to get the token
            // Aquí debes parsear la respuesta JSON para obtener el token
            // En este ejemplo, se asume que la respuesta contiene un campo 'token'
            return responseBody;
        }
    }

    // Agrega métodos para realizar llamadas a otros puntos finales de la API de Infobip
    // según tus necesidades
}
