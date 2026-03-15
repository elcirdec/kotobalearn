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

    private final KanjiService kanjiService;
    private final WordRepository  wordRepository;
    
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

    // GET /api/kanji/42/words?page=0&size=20
    // Mots qui contiennent ce kanji (via word_kanji)
    @GetMapping("/{id}/words")
    public ResponseEntity<Page<WordSummaryDto>> getWordsByKanji(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
 
        PageRequest pageable = PageRequest.of(page, size, Sort.by("wordId"));
        return ResponseEntity.ok(wordRepository.findByKanjiId(id, pageable));
    }
}