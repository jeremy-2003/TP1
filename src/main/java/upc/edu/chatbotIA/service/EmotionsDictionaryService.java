package upc.edu.chatbotIA.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import upc.edu.chatbotIA.model.EmotionsDictionary;
import upc.edu.chatbotIA.repository.EmotionsDictionaryRepository;

import java.util.List;

@Service
public class EmotionsDictionaryService {

    private final EmotionsDictionaryRepository emotionsDictionaryRepository;
    @Autowired
    public EmotionsDictionaryService(EmotionsDictionaryRepository emotionsDictionaryRepository) {
        this.emotionsDictionaryRepository = emotionsDictionaryRepository;
    }
    public void save(EmotionsDictionary emotionsDictionary) {
        emotionsDictionaryRepository.save(emotionsDictionary);
    }

    public List<EmotionsDictionary> findAll() {
        return (List<EmotionsDictionary>) emotionsDictionaryRepository.findAll();
    }

    public EmotionsDictionary findByEmotion(String emotion) {
        return emotionsDictionaryRepository.findByEmotion(emotion);
    }

}
