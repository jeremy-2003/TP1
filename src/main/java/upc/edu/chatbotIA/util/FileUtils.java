package upc.edu.chatbotIA.util;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {

    public static void writeByteArrayToFile(File file, byte[] bytes) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
        }
    }

    // Otros métodos útiles para operaciones de archivos pueden agregarse aquí
}
