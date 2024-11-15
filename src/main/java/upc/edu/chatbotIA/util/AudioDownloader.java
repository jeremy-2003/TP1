package upc.edu.chatbotIA.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import upc.edu.chatbotIA.ChatbotIaApplication;
import upc.edu.chatbotIA.config.TokenManagerConfig;

@Component
public class AudioDownloader {
    private String cookie;
    private String downloadLocation;
    private TokenManagerConfig tokenManagerConfig;

    @Value("${infobip.username}")
    private String username;

    @Value("${infobip.password}")
    private String password;
    public void setCookie(String cookie) {
        this.cookie = cookie;
    }
    public void setTokenManagerConfig(TokenManagerConfig tokenManagerConfig) {
        this.tokenManagerConfig = tokenManagerConfig;
    }
    public void setDownloadLocation(String downloadLocation) {
        this.downloadLocation = downloadLocation;
    }

    public File downloadAudio(String url) throws IOException {
        URL audioUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) audioUrl.openConnection();
        String token = tokenManagerConfig.getInfobipToken(username, password);
        connection.setRequestProperty("Cookie", "IbAuthCookie=" + token);

        Map<String, List<String>> headerFields = connection.getRequestProperties();
        System.out.println("Cookies enviadas:");
        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase("Cookie")) {
                List<String> cookies = entry.getValue();
                for (String cookie : cookies) {
                    System.out.println(cookie);
                }
            }
        }

        try (InputStream in = connection.getInputStream()) {
            // Crear un archivo temporal MP3
            File mp3File = File.createTempFile("audio_" + System.currentTimeMillis(), ".mp3");

            // Escribir los datos de audio en el archivo MP3
            try (FileOutputStream out = new FileOutputStream(mp3File)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            return mp3File;
        } finally {
            connection.disconnect();
        }
    }
}