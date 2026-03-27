package com.kotobalearn.backend.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kotobalearn.backend.model.JlptLevel;
import com.kotobalearn.backend.repository.JlptLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Importe les niveaux JLPT des kanji depuis le fichier kanji-data.json
 * de davidluzgouveia/kanji-data (MIT License).
 *
 * Source : https://github.com/davidluzgouveia/kanji-data
 * Fichier à placer dans : resources/data/kanji-data.json
 *
 * Format d'une entrée :
 * "勝": {
 *   "strokes": 12,
 *   "grade": 3,
 *   "freq": 185,
 *   "jlpt_old": 2,
 *   "jlpt_new": 3,   ← on utilise jlpt_new (liste communautaire post-2010)
 *   ...
 * }
 *
 * Correspondance jlpt_new : 1=N1, 2=N2, 3=N3, 4=N4, 5=N5
 * Grade : 1-6 = école primaire, 8 = secondaire (joyo), null = hors programme
 *
 * Ce service REMPLACE les niveaux JLPT kanji existants (issus de Kanjidic2
 * qui sont incomplets pour N1). Il met aussi à jour les grades scolaires.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KanjiDataImportService {

    private final DataSource          dataSource;
    private final JlptLevelRepository jlptLevelRepository;

    private static final String FILE_PATH = "/data/kanji-data.json";

    public String importKanjiData() throws Exception {
        log.info("Démarrage import JLPT kanji depuis kanji-data.json...");

        // 1. Pré-charger les JlptLevel (créer si absents)
        Map<Integer, Integer> jlptNumToId = loadOrCreateJlptLevels();
        log.info("Niveaux JLPT chargés : {}", jlptNumToId);

        // 2. Parser le JSON
        InputStream is = new ClassPathResource(FILE_PATH).getInputStream();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(is);

        int updatedJlpt  = 0;
        int updatedGrade = 0;
        int total        = 0;

        try (Connection conn = dataSource.getConnection()) {

            PreparedStatement updateJlpt = conn.prepareStatement(
                "UPDATE kanji SET jlpt_id = ? WHERE kanji_character = ?"
            );
            PreparedStatement clearJlpt = conn.prepareStatement(
                "UPDATE kanji SET jlpt_id = NULL WHERE kanji_character = ?"
            );
            PreparedStatement updateGrade = conn.prepareStatement(
                "UPDATE kanji SET kanji_grade = ? WHERE kanji_character = ?"
            );

            Set<Entry<String,JsonNode>> entries = root.properties();
            int batchSize = 0;

           for (Map.Entry<String, JsonNode> entry : entries) {
                String character = entry.getKey();
                JsonNode data    = entry.getValue();
                total++;

                // ── JLPT ───────────────────────────────────────────────────
                JsonNode jlptNode = data.get("jlpt_new");
                if (jlptNode != null && !jlptNode.isNull()) {
                    int jlptNum = jlptNode.asInt();
                    Integer jlptId = jlptNumToId.get(jlptNum);
                    if (jlptId != null) {
                        updateJlpt.setInt(1, jlptId);
                        updateJlpt.setString(2, character);
                        updateJlpt.addBatch();
                        updatedJlpt++;
                    }
                } else {
                    // Pas dans le JLPT → effacer un éventuel niveau existant
                    clearJlpt.setString(1, character);
                    clearJlpt.addBatch();
                }

                // ── Grade scolaire ─────────────────────────────────────────
                JsonNode gradeNode = data.get("grade");
                if (gradeNode != null && !gradeNode.isNull()) {
                    int grade = gradeNode.asInt();
                    // grade 8 = joyo secondaire, on le garde tel quel
                    // grade 9 = jinmeiyo (noms propres), optionnel
                    updateGrade.setInt(1, grade);
                    updateGrade.setString(2, character);
                    updateGrade.addBatch();
                    updatedGrade++;
                }

                // Flush par batch
                if (++batchSize % 500 == 0) {
                    updateJlpt.executeBatch();
                    clearJlpt.executeBatch();
                    updateGrade.executeBatch();
                    log.info("  {} entrées traitées...", total);
                }
            }

            // Flush final
            updateJlpt.executeBatch();
            clearJlpt.executeBatch();
            updateGrade.executeBatch();

            updateJlpt.close();
            clearJlpt.close();
            updateGrade.close();
        }

        String result = String.format(
            "Import kanji-data terminé — %d kanji avec JLPT mis à jour, " +
            "%d grades mis à jour, sur %d entrées au total",
            updatedJlpt, updatedGrade, total
        );
        log.info(result);
        return result;
    }

    /**
     * Charge ou crée les entrées jlpt_level pour N1-N5.
     * Retourne une map : numéro (1-5) → jlpt_id en base.
     */
    private Map<Integer, Integer> loadOrCreateJlptLevels() {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            String code = "N" + i;
            JlptLevel level = jlptLevelRepository.findByJlptCode(code)
                .orElseGet(() -> {
                    JlptLevel l = new JlptLevel();
                    l.setJlptCode(code);
                    l.setJlptDescription("JLPT Level " + code);
                    return jlptLevelRepository.save(l);
                });
            map.put(i, level.getJlptId());
        }
        return map;
    }
}