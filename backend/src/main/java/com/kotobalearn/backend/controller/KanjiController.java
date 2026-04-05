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
     * GET /api/kanji
     * GET /api/kanji?jlpt=N5
     * GET /api/kanji?gradeGroup=primaire     ← grades 1-6 (école primaire)
     * GET /api/kanji?gradeGroup=secondaire   ← grade 8 (collège/lycée, joyo)
     * GET /api/kanji?gradeGroup=prenoms      ← grade 9 (jinmeiyō, prénoms)
     * GET /api/kanji?radicalIds=5,12,34
     * GET /api/kanji?search=water
     */
    @GetMapping
    public List<KanjiSummaryDto> getAll(
        @RequestParam(required = false) String        jlpt,
        @RequestParam(required = false) String        gradeGroup,
        @RequestParam(required = false) Integer       strokes,
        @RequestParam(required = false) List<Integer> radicalIds,
        @RequestParam(required = false) String        search
    ) {
        List<Integer> grades = resolveGrades(gradeGroup);
        return kanjiService.findByCriteria(jlpt, grades, strokes, radicalIds, search);
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

    /**
     * GET /api/kanji/stroke-counts
     * GET /api/kanji/stroke-counts?jlpt=N1
     * GET /api/kanji/stroke-counts?gradeGroup=primaire
     * GET /api/kanji/stroke-counts?jlpt=N2&gradeGroup=secondaire
     *
     * Retourne uniquement les nombres de traits qui existent pour les filtres actifs.
     * Évite d'afficher des options inutiles dans le sélecteur (ex: 29 traits n'existe pas en N5).
     */
    @GetMapping("/stroke-counts")
    public List<Integer> getStrokeCounts(
        @RequestParam(required = false) String jlpt,
        @RequestParam(required = false) String gradeGroup
    ) {
        List<Integer> grades = resolveGrades(gradeGroup);
        return kanjiService.findDistinctStrokeCounts(jlpt, grades);
    }

    /**
     * Convertit un gradeGroup en liste de grades SQL.
     * primaire   → 1, 2, 3, 4, 5, 6
     * secondaire → 8
     * prenoms    → 9
     * null       → null (pas de filtre)
     */
    private List<Integer> resolveGrades(String gradeGroup) {
        if (gradeGroup == null || gradeGroup.isBlank()) return null;
        return switch (gradeGroup.toLowerCase()) {
            case "primaire"   -> List.of(1, 2, 3, 4, 5, 6);
            case "secondaire" -> List.of(8);
            case "prenoms"    -> List.of(9);
            default           -> null;
        };
    }
}