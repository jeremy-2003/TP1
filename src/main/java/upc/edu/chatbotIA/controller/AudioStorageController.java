package upc.edu.chatbotIA.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import upc.edu.chatbotIA.service.AudioStorageService;

@RestController
@RequestMapping("/azure")
public class AudioStorageController {

    private final AudioStorageService audioStorageService;

    public AudioStorageController(AudioStorageService audioStorageService) {
        this.audioStorageService = audioStorageService;
    }

    @PostMapping("/upload-audio")
    public String uploadAudio(@RequestParam("file") MultipartFile file) {
        String audioUrl = audioStorageService.uploadAudio(file);
        return "Archivo de audio subido correctamente. URL: " + audioUrl.replace(" ", "");
    }

    @DeleteMapping("/delete-audio")
    public String deleteAudio(@RequestParam("fileName") String fileName) {
        audioStorageService.deleteAudio(fileName);
        return "Archivo de audio eliminado correctamente";
    }
}
