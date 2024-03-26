package upc.edu.chatbotIA.service;
import com.theokanning.openai.service.OpenAiService;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.StatusLine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Service
public class TranscriptionService {
    @Value("${openai.api.token}")
    private String openAiApiKey;
    private final OpenAiService openAiService;
    private static final String URL = "https://api.openai.com/v1/audio/transcriptions";
    private static final String MODEL = "whisper-1";

    public TranscriptionService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public String transcribeAudio(File audioFile) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(URL);
            httpPost.setHeader("Authorization", "Bearer " + openAiApiKey);

            HttpEntity entity = MultipartEntityBuilder.create()
                    .setContentType(ContentType.MULTIPART_FORM_DATA)
                    .addPart("file", new FileBody(audioFile, ContentType.DEFAULT_BINARY))
                    .addPart("model", new StringBody(MODEL, ContentType.DEFAULT_TEXT))
                    .addPart("response_format", new StringBody("text", ContentType.DEFAULT_TEXT))
                    .addPart("language", new StringBody("es", ContentType.DEFAULT_TEXT))
                    .build();
            httpPost.setEntity(entity);

            return client.execute(httpPost, response -> {
                System.out.println("Status: " + new StatusLine(response));
                return EntityUtils.toString(response.getEntity());
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}