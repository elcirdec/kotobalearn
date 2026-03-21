package com.kotobalearn.backend.controller;

import com.kotobalearn.backend.importer.JMdictEnrichService;
import com.kotobalearn.backend.importer.JMdictImportService;
import com.kotobalearn.backend.importer.KanjiAliveImportService;
import com.kotobalearn.backend.importer.KanjidicImportService;
import com.kotobalearn.backend.importer.KradfileImportService;
import com.kotobalearn.backend.importer.JlptVocabImportService;
import com.kotobalearn.backend.importer.RadkfileStrokeImportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final KanjiAliveImportService importService;
    private final KanjidicImportService kanjidicImportService;
    private final JMdictImportService     jmdictImportService;
    private final JMdictEnrichService     jmDictEnrichService;
    private final JlptVocabImportService  jlptVocabImportService;
    private final KradfileImportService    kradfileImportService;
    private final RadkfileStrokeImportService radkfileStrokeImportService;
    // POST http://localhost:8080/api/admin/import/kanjialive
    @PostMapping("/import/kanjialive")
    public ResponseEntity<String> importKanjiAlive() {
        try {
            importService.importAll();
            return ResponseEntity.ok("Import terminé avec succès.");
        } catch (Exception e) {
            log.error("Import failed", e);
            return ResponseEntity.internalServerError()
                .body("Erreur : " + e.getMessage());
        }
    }

    @PostMapping("/import/kanjidic")
    public ResponseEntity<String> importKanjidic() {
        String result = kanjidicImportService.importAllKanji();
        return ResponseEntity.ok(result);
    }
    @PostMapping("/import/jmdict")
    public ResponseEntity<String> importJMdict() {
        String result = jmdictImportService.importWords();
        return ResponseEntity.ok(result);
    }

    // ── Enrichissements ────────────────────────────────────────────────────
 
    /**
     * POST /api/admin/enrich/jmdict-seq
     * Lit le JMdict XML et met à jour word_jmdict_seq + word_frequency_rank.
     */
    @PostMapping("/enrich/jmdict-seq")
    public ResponseEntity<String> enrichJmdictSeq() throws Exception {
        return ResponseEntity.ok(jmDictEnrichService.enrich());
    }
 
    /**
     * POST /api/admin/enrich/jlpt-vocab
     * Assigne les niveaux JLPT aux mots depuis les CSV stephenmk.
     * Prérequis : resources/data/jlpt/original_data/{n1-n5}.csv
     */
    @PostMapping("/enrich/jlpt-vocab")
    public ResponseEntity<String> enrichJlptVocab() throws Exception {
        return ResponseEntity.ok(jlptVocabImportService.importJlpt());
    }

    /**
     * POST /api/admin/enrich/kradfile
     * Remplit la table kanji_component avec tous les composants visuels
     * de chaque kanji (KRADFILE + KRADFILE2, EDRDG CC BY-SA).
     * Met aussi à jour rad_id avec le radical principal (premier composant).
     * Prérequis : resources/data/kradfile et resources/data/kradfile2
     */
    @PostMapping("/enrich/kradfile")
    public ResponseEntity<String> enrichKradfile() throws Exception {
        return ResponseEntity.ok(kradfileImportService.importKradfile());
    }

    /**
     * POST /api/admin/enrich/radkfile-strokes
     * Met à jour rad_strokes sur tous les composants depuis RADKFILE.
     * Prérequis : resources/data/radkfile et resources/data/radkfile2
     * À lancer APRÈS enrich/kradfile.
     */
    @PostMapping("/enrich/radkfile-strokes")
    public ResponseEntity<String> enrichRadkfileStrokes() throws Exception {
        return ResponseEntity.ok(radkfileStrokeImportService.importStrokes());
    }
}