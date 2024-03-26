package upc.edu.chatbotIA.service;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AudioStorageService {
    private final BlobContainerClient containerClient;

    public AudioStorageService(@Value("${azure.storage.connection-string}") String connectionString,
                               @Value("${azure.storage.container-name}") String containerName) {
        BlobServiceClientBuilder builder = new BlobServiceClientBuilder();
        builder.connectionString(connectionString);
        containerClient = builder.buildClient().getBlobContainerClient(containerName);
    }

    public String uploadAudio(MultipartFile file) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(file.getOriginalFilename());

            // Establecer el tipo de contenido correcto
            BlobHttpHeaders headers = new BlobHttpHeaders();
            headers.setContentType("audio/ogg");

            blobClient.upload(file.getInputStream(), file.getSize(), true);
            blobClient.setHttpHeaders(headers);

            return blobClient.getBlobUrl();
        } catch (Exception e) {
            throw new RuntimeException("Error al subir el archivo de audio", e);
        }
    }

    public void deleteAudio(String fileName) {
        try {
            BlobClient blobClient = containerClient.getBlobClient(fileName);
            blobClient.delete();
        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar el archivo de audio", e);
        }
    }
}
