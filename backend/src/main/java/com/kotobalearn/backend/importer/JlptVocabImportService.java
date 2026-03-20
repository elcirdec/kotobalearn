package com.kotobalearn.backend.importer;

import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Importe les niveaux JLPT depuis les fichiers CSV du dossier original_data
 * du repo stephenmk/yomitan-jlpt-vocab.
 *
 * Structure attendue dans resources/data/jlpt/original_data/ :
 *   n1.csv, n2.csv, n3.csv, n4.csv, n5.csv
 *
 * Format CSV (séparateur tabulation) :
 *   jmdict_seq  kana  kanji  waller_definition
 *   1565440     ああ   嗚呼    Ah!, Oh!, Alas!
 *
 * Matching (dans l'ordre) :
 *   1. word_jmdict_seq = jmdict_seq  (si enrichissement JMdict déjà fait)
 *   2. word_japanese   = kanji       (fallback sur la forme écrite)
 *   3. word_japanese   = kana        (fallback sur la lecture si pas de kanji)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JlptVocabImportService {

    private final DataSource dataSource;

    private static final String BASE_PATH = "/data/jlpt/original_data/";
    private static final int BATCH_SIZE = 500;

    public String importJlpt() throws Exception {
        log.info("Démarrage import JLPT vocabulaire depuis original_data CSVs...");

        Map<String, Integer> jlptIds = loadJlptIds();
        log.info("Niveaux JLPT en base : {}", jlptIds.keySet());

        int total = 0;

        for (String level : new String[]{"N5", "N4", "N3", "N2", "N1"}) {
            Integer jlptId = jlptIds.get(level);
            if (jlptId == null) {
                log.warn("Niveau {} non trouvé en base, ignoré", level);
                continue;
            }
            int count = processLevel(level, jlptId);
            log.info("JLPT {} : {} mots mis à jour", level, count);
            total += count;
        }

        String result = "Import JLPT terminé — " + total + " mots mis à jour";
        log.info(result);
        return result;
    }

    private int processLevel(String level, int jlptId) throws Exception{
        String filename = BASE_PATH + level.toLowerCase() + ".csv";
        InputStream is = getClass().getResourceAsStream(filename);
        if (is == null) {
            log.warn("Fichier introuvable : {}", filename);
            return 0;
        }

        int count = 0;

        try (Connection conn = dataSource.getConnection();
             CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            PreparedStatement bySeq = conn.prepareStatement("""
                UPDATE word
                SET word_jlpt_id = ?
                WHERE word_jmdict_seq = ?
                  AND word_jlpt_id IS NULL
                """);

            PreparedStatement byKanji = conn.prepareStatement("""
                UPDATE word
                SET word_jlpt_id = ?
                WHERE word_japanese = ?
                  AND word_jlpt_id IS NULL
                """);

            int batchSeq = 0, batchKanji = 0;
            String[] cols;
            boolean firstLine = true;

            while ((cols = reader.readNext()) != null) {
                if (firstLine) {
                    firstLine = false;
                    // Ignorer l'en-tête
                    if (cols.length > 0 && (cols[0].startsWith("jmdict_seq") || cols[0].startsWith("seq"))) {
                        continue;
                    }
                }
                if (cols.length < 2) continue;

                int seq = 0;
                String kana = cols.length > 1 ? cols[1].trim() : "";
                String kanji = cols.length > 2 ? cols[2].trim() : "";

                try {
                    seq = Integer.parseInt(cols[0].trim());
                } catch (NumberFormatException e) {
                    continue;
                }

                // 1. Matching par seq JMdict
                if (seq > 0) {
                    bySeq.setInt(1, jlptId);
                    bySeq.setInt(2, seq);
                    bySeq.addBatch();
                    if (++batchSeq % BATCH_SIZE == 0) {
                        count += sum(bySeq.executeBatch());
                    }
                }

                // 2. Matching par forme écrite (kanji ou kana)
                String form = !kanji.isEmpty() ? kanji : kana;
                if (!form.isEmpty()) {
                    byKanji.setInt(1, jlptId);
                    byKanji.setString(2, form);
                    byKanji.addBatch();
                    if (++batchKanji % BATCH_SIZE == 0) {
                        count += sum(byKanji.executeBatch());
                    }
                }

                // 3. Matching par kana séparément si kanji différent
                if (!kanji.isEmpty() && !kana.isEmpty() && !kana.equals(kanji)) {
                    byKanji.setInt(1, jlptId);
                    byKanji.setString(2, kana);
                    byKanji.addBatch();
                    if (++batchKanji % BATCH_SIZE == 0) {
                        count += sum(byKanji.executeBatch());
                    }
                }
            }

            // Flush finaux
            if (batchSeq % BATCH_SIZE != 0) {
                count += sum(bySeq.executeBatch());
            }
            if (batchKanji % BATCH_SIZE != 0) {
                count += sum(byKanji.executeBatch());
            }

            bySeq.close();
            byKanji.close();
        }

        return count;
    }

    private Map<String, Integer> loadJlptIds() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT jlpt_id, jlpt_code FROM jlpt_level");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("jlpt_code"), rs.getInt("jlpt_id"));
            }
        }
        return map;
    }

    private int sum(int[] results) {
        int s = 0;
        for (int r : results) if (r > 0) s++;
        return s;
    }
}