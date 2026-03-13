package com.kotobalearn.backend.controller;

import com.kotobalearn.backend.dto.WordDetailDto;
import com.kotobalearn.backend.dto.WordSummaryDto;
import com.kotobalearn.backend.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    /**
     * GET /api/words
     * GET /api/words?jlpt=N5
     * GET /api/words?tag=food
     * GET /api/words?tagType=pos
     * GET /api/words?search=食べ
     * GET /api/words?jlpt=N5&search=食べ
     * GET /api/words?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<WordSummaryDto>> getWords(
        @RequestParam(required = false)              String jlpt,
        @RequestParam(required = false)              String tag,
        @RequestParam(required = false)              String tagType,
        @RequestParam(required = false)              String search,
        @RequestParam(defaultValue = "0")            int    page,
        @RequestParam(defaultValue = "20")           int    size
    ) {
        return ResponseEntity.ok(
            wordService.findAll(jlpt, tag, tagType, search, page, size)
        );
    }

    /**
     * GET /api/words/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<WordDetailDto> getWord(@PathVariable Integer id) {
        return ResponseEntity.ok(wordService.findById(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity.notFound().build();
    }
}