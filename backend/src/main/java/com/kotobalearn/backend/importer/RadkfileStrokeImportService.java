package com.kotobalearn.backend.importer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

/**
 * Lit RADKFILE / RADKFILE2 pour extraire le nombre de traits de chaque composant.
 *
 * Format : "$ 一 1" = $ caractère nb_traits
 *
 * Met à jour radical.rad_strokes pour tous les composants qui n'ont pas encore
 * de valeur (NULL), c'est-à-dire les composants KRADFILE insérés sans traits.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RadkfileStrokeImportService {

    private final DataSource dataSource;

    private static final String[] FILES = {
        "/data/radkfile",
        "/data/radkfile2"
    };

    public String importStrokes() throws Exception {
        log.info("Démarrage import traits depuis RADKFILE...");

        // Lire les deux fichiers → map character → strokes
        Map<String, Integer> strokeMap = new HashMap<>();
        for (String filePath : FILES) {
            readFile(filePath, strokeMap);
        }
        log.info("{} composants avec nombre de traits trouvés", strokeMap.size());

        // Mettre à jour radical.rad_strokes
        int updated = updateStrokes(strokeMap);

        String msg = String.format(
            "Import RADKFILE traits terminé — %d composants mis à jour", updated
        );
        log.info(msg);
        return msg;
    }

    private void readFile(String filePath, Map<String, Integer> strokeMap) throws Exception {
        InputStream is = getClass().getResourceAsStream(filePath);
        if (is == null) { log.warn("Fichier introuvable : {}", filePath); return; }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, Charset.forName("EUC-JP")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.isBlank()) continue;
                // Format : "$ 一 1" ou "$ 一 1 code"
                if (line.startsWith("$ ")) {
                    String[] parts = line.split(" ");
                    if (parts.length < 3) continue;
                    String character = parts[1].trim();
                    try {
                        int strokes = Integer.parseInt(parts[2].trim());
                        strokeMap.put(character, strokes);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    private int updateStrokes(Map<String, Integer> strokeMap) throws Exception {
        int updated = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE radical SET rad_strokes = ? WHERE rad_character = ?")) {

            for (Map.Entry<String, Integer> entry : strokeMap.entrySet()) {
                ps.setInt(1, entry.getValue());
                ps.setString(2, entry.getKey());
                ps.addBatch();
            }
            int[] res = ps.executeBatch();
            for (int r : res) if (r > 0) updated++;
        }
        return updated;
    }
}