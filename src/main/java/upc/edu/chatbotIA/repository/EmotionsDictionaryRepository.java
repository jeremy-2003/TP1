package upc.edu.chatbotIA.repository;

import org.springframework.data.repository.CrudRepository;
import upc.edu.chatbotIA.model.EmotionsDictionary;

public interface EmotionsDictionaryRepository extends CrudRepository<EmotionsDictionary, Long> {
    EmotionsDictionary findByEmotion(String emotion);
}
