package com.kotobalearn.backend.controller;

import com.kotobalearn.backend.dto.KanjiDetailDto;
import com.kotobalearn.backend.dto.KanjiSummaryDto;
import com.kotobalearn.backend.dto.WordSummaryDto;
import com.kotobalearn.backend.repository.WordRepository;
import com.kotobalearn.backend.service.KanjiService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kanji")
@RequiredArgsConstructor
public class KanjiController {

    private final KanjiService   kanjiService;
    private final WordRepository wordRepository;

    /**
     * GET /api/kanji?jlpt=N5&grade=1&strokes=3&radicalIds=5,12&search=water
     * Tous les paramètres sont optionnels et combinables.
     * radicalIds = liste d'IDs séparés par virgule
     */
    @GetMapping
    public List<KanjiSummaryDto> getAll(
        @RequestParam(required = false) String       jlpt,
        @RequestParam(required = false) Integer      grade,
        @RequestParam(required = false) Integer      strokes,
        @RequestParam(required = false) List<Integer> radicalIds,
        @RequestParam(required = false) String       search
    ) {
        return kanjiService.findByCriteria(jlpt, grade, strokes, radicalIds, search);
    }

    @GetMapping("/{id}")
    public ResponseEntity<KanjiDetailDto> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(kanjiService.findById(id));
    }

    @GetMapping("/character/{character}")
    public ResponseEntity<KanjiDetailDto> getByCharacter(@PathVariable String character) {
        return ResponseEntity.ok(kanjiService.findByCharacter(character));
    }

    @GetMapping("/{id}/words")
    public ResponseEntity<Page<WordSummaryDto>> getWordsByKanji(
        @PathVariable Integer id,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
            wordRepository.findByKanjiId(id, PageRequest.of(page, size, Sort.by("wordId")))
        );
    }
}