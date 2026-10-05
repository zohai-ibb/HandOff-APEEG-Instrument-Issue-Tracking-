package com.example.APEEG.controller;

import com.example.APEEG.model.ScientistList;
import com.example.APEEG.service.ScientistListService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scientist-list")
@CrossOrigin(origins = "*")
public class ScientistListController {

    private final ScientistListService scientistListService;

    public ScientistListController(ScientistListService scientistListService) {
        this.scientistListService = scientistListService;
    }

    @PostMapping
    public ResponseEntity<?> addScientist(@RequestBody ScientistList scientist) {
        try {
            ScientistList saved = scientistListService.addScientist(scientist);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyScientists() {
        try {
            List<ScientistList> list = scientistListService.getMyScientists();
            return ResponseEntity.ok(list);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}