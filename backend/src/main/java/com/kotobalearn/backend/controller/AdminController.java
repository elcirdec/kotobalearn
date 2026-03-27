package com.kotobalearn.backend.controller;

import com.kotobalearn.backend.importer.JMdictEnrichService;
import com.kotobalearn.backend.importer.JMdictImportService;
import com.kotobalearn.backend.importer.KanjiAliveImportService;
import com.kotobalearn.backend.importer.KanjiDataImportService;
import com.kotobalearn.backend.importer.KanjidicImportService;
import com.kotobalearn.backend.importer.KradfileImportService;
import com.kotobalearn.backend.importer.JlptVocabImportService;
import com.kotobalearn.backend.importer.RadkfileStrokeImportService;
import com.kotobalearn.backend.importer.RadicalNameEnrichService;

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

    private final KanjiAliveImportService     importService;
    private final KanjidicImportService       kanjidicImportService;
    private final JMdictImportService         jmdictImportService;
    private final JMdictEnrichService         jmDictEnrichService;
    private final JlptVocabImportService      jlptVocabImportService;
    private final KradfileImportService       kradfileImportService;
    private final RadkfileStrokeImportService radkfileStrokeImportService;
    private final RadicalNameEnrichService    radicalNameEnrichService;
    private final KanjiDataImportService      kanjiDataImportService;

    // ── ÉTAPE 1 : Imports de base ──────────────────────────────────────────

    /** Import des radicaux KanjiAlive + kanji de base (médias, lectures, exemples audio) */
    @PostMapping("/import/kanjialive")
    public ResponseEntity<String> importKanjiAlive() {
        try {
            importService.importAll();
            return ResponseEntity.ok("KanjiAlive import terminé.");
        } catch (Exception e) {
            log.error("Import failed", e);
            return ResponseEntity.internalServerError().body("Erreur : " + e.getMessage());
        }
    }

    /** Import complet des kanji depuis Kanjidic2 (strokes, grade, lectures, sens…) */
    @PostMapping("/import/kanjidic")
    public ResponseEntity<String> importKanjidic() {
        return ResponseEntity.ok(kanjidicImportService.importAllKanji());
    }

    /** Import du vocabulaire JMdict (~215 000 mots avec lectures et traductions) */
    @PostMapping("/import/jmdict")
    public ResponseEntity<String> importJMdict() {
        return ResponseEntity.ok(jmdictImportService.importWords());
    }

    // ── ÉTAPE 2 : Enrichissements mots ────────────────────────────────────

    /**
     * Lit le JMdict XML et met à jour word_jmdict_seq + word_frequency_rank.
     * Prérequis : import/jmdict déjà effectué.
     */
    @PostMapping("/enrich/jmdict-seq")
    public ResponseEntity<String> enrichJmdictSeq() throws Exception {
        return ResponseEntity.ok(jmDictEnrichService.enrich());
    }

    /**
     * Assigne les niveaux JLPT aux MOTS depuis les CSV stephenmk/yomitan-jlpt-vocab.
     * Prérequis : enrich/jmdict-seq déjà effectué.
     * Fichiers : resources/data/jlpt/original_data/{n1-n5}.csv
     */
    @PostMapping("/enrich/jlpt-vocab")
    public ResponseEntity<String> enrichJlptVocab() throws Exception {
        return ResponseEntity.ok(jlptVocabImportService.importJlpt());
    }

    // ── ÉTAPE 3 : Enrichissements kanji ───────────────────────────────────

    /**
     * Met à jour les niveaux JLPT des KANJI + grades scolaires
     * depuis kanji-data.json (davidluzgouveia, MIT License).
     * Source : https://github.com/davidluzgouveia/kanji-data
     * Fichier : resources/data/kanji-data.json
     * Utilise jlpt_new (liste communautaire post-2010, N1 complet).
     * REMPLACE les niveaux JLPT issus de Kanjidic2 (incomplets pour N1).
     */
    @PostMapping("/enrich/kanji-jlpt")
    public ResponseEntity<String> enrichKanjiJlpt() throws Exception {
        return ResponseEntity.ok(kanjiDataImportService.importKanjiData());
    }

    // ── ÉTAPE 4 : Radicaux et composants ──────────────────────────────────

    /**
     * Remplit kanji_component avec tous les composants visuels de chaque kanji.
     * Met aussi à jour rad_id avec le radical principal.
     * Source : KRADFILE + KRADFILE2 (EDRDG, CC BY-SA)
     * Fichiers : resources/data/kradfile et resources/data/kradfile2
     */
    @PostMapping("/enrich/kradfile")
    public ResponseEntity<String> enrichKradfile() throws Exception {
        return ResponseEntity.ok(kradfileImportService.importKradfile());
    }

    /**
     * Met à jour rad_strokes sur les composants depuis RADKFILE.
     * Prérequis : enrich/kradfile déjà effectué.
     * Fichiers : resources/data/radkfile et resources/data/radkfile2
     */
    @PostMapping("/enrich/radkfile-strokes")
    public ResponseEntity<String> enrichRadkfileStrokes() throws Exception {
        return ResponseEntity.ok(radkfileStrokeImportService.importStrokes());
    }

    /**
     * Enrichit les composants KRADFILE avec les noms et sens KanjiAlive.
     * Prérequis : enrich/kradfile + enrich/radkfile-strokes déjà effectués.
     * Fichier : resources/data/japanese-radicals.csv
     */
    @PostMapping("/enrich/radical-names")
    public ResponseEntity<String> enrichRadicalNames() throws Exception {
        return ResponseEntity.ok(radicalNameEnrichService.enrichRadicalNames());
    }
}