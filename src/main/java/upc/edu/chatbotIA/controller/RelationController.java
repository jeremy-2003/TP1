package upc.edu.chatbotIA.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import upc.edu.chatbotIA.model.Relation;
import upc.edu.chatbotIA.service.RelationService;

import java.util.List;

@RestController("/relations")
public class RelationController {

    private final RelationService relationService;

    @Autowired
    public RelationController(RelationService relationService) {
        this.relationService = relationService;
    }

    @GetMapping
    public ResponseEntity<List<Relation>> getAllRelations() {
        List<Relation> relations = relationService.getAllRelations();
        return new ResponseEntity<>(relations, HttpStatus.OK);
    }
}