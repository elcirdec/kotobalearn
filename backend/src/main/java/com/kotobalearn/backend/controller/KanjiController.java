package com.kotobalearn.backend.controller;

import com.kotobalearn.backend.dto.KanjiDetailDto;
import com.kotobalearn.backend.dto.KanjiSummaryDto;
import com.kotobalearn.backend.service.KanjiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/kanji")
@RequiredArgsConstructor
public class KanjiController {

    private final KanjiService kanjiService;

    // GET /api/kanji
    // GET /api/kanji?jlpt=N5
    // GET /api/kanji?grade=1
    @GetMapping
    public List<KanjiSummaryDto> getAll(
            @RequestParam(required = false) String jlpt,
            @RequestParam(required = false) Integer grade) {

        if (jlpt != null)  return kanjiService.findByJlpt(jlpt.toUpperCase());
        if (grade != null) return kanjiService.findByGrade(grade);
        return kanjiService.findAll();
    }

    // GET /api/kanji/42
    @GetMapping("/{id}")
    public ResponseEntity<KanjiDetailDto> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(kanjiService.findById(id));
    }

    // GET /api/kanji/character/愛
    @GetMapping("/character/{character}")
    public ResponseEntity<KanjiDetailDto> getByCharacter(@PathVariable String character) {
        return ResponseEntity.ok(kanjiService.findByCharacter(character));
    }
}