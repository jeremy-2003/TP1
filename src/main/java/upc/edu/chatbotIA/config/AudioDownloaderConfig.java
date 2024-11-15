package upc.edu.chatbotIA.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import upc.edu.chatbotIA.util.AudioDownloader;
@Configuration
public class AudioDownloaderConfig {
    @Value("${audioDownloader.cookie}")
    private String cookie;
    @Autowired
    private TokenManagerConfig tokenManagerConfig;
    @Value("${audioDownloader.downloadLocation}")
    private String downloadLocation;

    @Bean
    public AudioDownloader audioDownloader() {
        AudioDownloader downloader = new AudioDownloader();
        downloader.setCookie(cookie);
        downloader.setDownloadLocation(downloadLocation);
        downloader.setTokenManagerConfig(tokenManagerConfig); // Inject TokenManagerConfig
        return downloader;
    }
}