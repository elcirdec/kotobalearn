package com.kotobalearn.backend.controller;

import com.kotobalearn.backend.dto.WordDetailDto;
import com.kotobalearn.backend.dto.WordSummaryDto;
import com.kotobalearn.backend.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    /**
     * GET /api/words?search=cat          → ranké : exact > commence par > contient
     * GET /api/words?jlpt=N5
     * GET /api/words?tags=food,n&tagMode=and
     * GET /api/words?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<WordSummaryDto>> getWords(
        @RequestParam(required = false)    String       jlpt,
        @RequestParam(required = false)    List<String> tags,
        @RequestParam(defaultValue = "or") String       tagMode,
        @RequestParam(required = false)    String       search,
        @RequestParam(defaultValue = "0")  int          page,
        @RequestParam(defaultValue = "20") int          size
    ) {
        return ResponseEntity.ok(
            wordService.findAll(jlpt, tags, tagMode, search, page, size)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<WordDetailDto> getWord(@PathVariable Integer id) {
        return ResponseEntity.ok(wordService.findById(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity.notFound().build();
    }
}