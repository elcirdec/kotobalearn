package com.kotobalearn.backend.controller;

import com.kotobalearn.backend.importer.JMdictImportService;
import com.kotobalearn.backend.importer.KanjiAliveImportService;
import com.kotobalearn.backend.importer.KanjidicImportService;

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
}