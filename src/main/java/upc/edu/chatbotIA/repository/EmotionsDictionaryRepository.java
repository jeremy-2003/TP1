package upc.edu.chatbotIA.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import upc.edu.chatbotIA.model.EmotionsDictionary;
@Repository
public interface EmotionsDictionaryRepository extends CrudRepository<EmotionsDictionary, Long> {
    EmotionsDictionary findByEmotion(String emotion);
}
