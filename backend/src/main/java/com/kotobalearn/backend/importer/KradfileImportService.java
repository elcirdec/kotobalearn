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
import java.sql.ResultSet;
import java.util.*;

/**
 * Importe les composants visuels des kanji depuis KRADFILE / KRADFILE2 (EDRDG, CC BY-SA).
 *
 * Format : "漢字 : composant1 composant2 ..."
 *
 * Exemple : 亜 : ｜ 一 口
 *   → insère ｜, 一, 口 dans radical (si absents)
 *   → crée les liens 亜↔｜, 亜↔一, 亜↔口 dans kanji_component
 *
 * Pas de dépendance aux radicaux KanjiAlive.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KradfileImportService {

    private final DataSource dataSource;

    private static final String[] FILES = {
        "/data/kradfile",
        "/data/kradfile2"
    };
    private static final int BATCH_SIZE = 500;

    public String importKradfile() throws Exception {
        log.info("Démarrage import KRADFILE (composants kanji)...");

        // ── Passe 1 : lire tous les fichiers ─────────────────────────────
        Map<String, List<String>> kanjiToComps = new LinkedHashMap<>();
        for (String filePath : FILES) {
            readFile(filePath, kanjiToComps);
        }
        log.info("{} kanji lus depuis KRADFILE", kanjiToComps.size());

        // ── Passe 2 : collecter les composants uniques ───────────────────
        Set<String> allComponents = new LinkedHashSet<>();
        for (List<String> comps : kanjiToComps.values()) {
            allComponents.addAll(comps);
        }
        log.info("{} composants uniques trouvés", allComponents.size());

        // ── Passe 3 : insérer les composants manquants dans radical ───────
        Map<String, Integer> componentToId = loadOrInsertComponents(allComponents);
        log.info("{} composants disponibles en base", componentToId.size());

        // ── Passe 4 : charger les IDs des kanji ──────────────────────────
        Map<String, Integer> kanjiToId = loadKanjiMap();
        log.info("{} kanji disponibles en base", kanjiToId.size());

        // ── Passe 5 : créer les liaisons kanji_component ─────────────────
        int[] result = insertLinks(kanjiToComps, kanjiToId, componentToId);

        String msg = String.format(
            "Import KRADFILE terminé — %d composants en base, " +
            "%d liens kanji↔composant créés, %d kanji avec radical principal",
            componentToId.size(), result[0], result[1]
        );
        log.info(msg);
        return msg;
    }

    // ── Lecture EUC-JP ────────────────────────────────────────────────────

    private void readFile(String filePath, Map<String, List<String>> result) throws Exception {
        InputStream is = getClass().getResourceAsStream(filePath);
        if (is == null) { log.warn("Fichier introuvable : {}", filePath); return; }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, Charset.forName("EUC-JP")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.isBlank()) continue;
                int colonIdx = line.indexOf(" : ");
                if (colonIdx < 0) continue;
                String kanji    = line.substring(0, colonIdx).trim();
                String compsPart = line.substring(colonIdx + 3).trim();
                if (kanji.isEmpty() || compsPart.isEmpty()) continue;

                List<String> comps = new ArrayList<>();
                for (String c : compsPart.split(" ")) {
                    c = c.trim();
                    if (!c.isEmpty()) comps.add(c);
                }
                if (!comps.isEmpty()) result.put(kanji, comps);
            }
        }
    }

    // ── Insérer les composants manquants, retourner character→id ─────────

    private Map<String, Integer> loadOrInsertComponents(Set<String> components) throws Exception {
        Map<String, Integer> map = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {

            // Charger ceux qui existent déjà
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT rad_id, rad_character FROM radical WHERE rad_character IS NOT NULL");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("rad_character"), rs.getInt("rad_id"));
                }
            }

            // Insérer les manquants un par un
            // ON CONFLICT nécessite la contrainte UNIQUE sur rad_character (V11)
            PreparedStatement insertOne = conn.prepareStatement(
                "INSERT INTO radical (rad_character, rad_type) " +
                "VALUES (?, 'component') ON CONFLICT (rad_character) DO NOTHING"
            );
            for (String comp : components) {
                if (!map.containsKey(comp)) {
                    insertOne.setString(1, comp);
                    insertOne.executeUpdate();
                }
            }
            insertOne.close();

            // Recharger tous les IDs
            map.clear();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT rad_id, rad_character FROM radical WHERE rad_character IS NOT NULL");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("rad_character"), rs.getInt("rad_id"));
                }
            }
        }

        return map;
    }

    // ── Créer les liens kanji_component ───────────────────────────────────

    private int[] insertLinks(
        Map<String, List<String>> kanjiToComps,
        Map<String, Integer>      kanjiToId,
        Map<String, Integer>      componentToId
    ) throws Exception {

        int links   = 0;
        int updated = 0;

        try (Connection conn = dataSource.getConnection()) {

            PreparedStatement insertLink = conn.prepareStatement("""
                INSERT INTO kanji_component (kanji_id, radical_id, kc_position)
                VALUES (?, ?, ?)
                ON CONFLICT (kanji_id, radical_id) DO NOTHING
                """);

            PreparedStatement updateRadId = conn.prepareStatement("""
                UPDATE kanji SET rad_id = ?
                WHERE kanji_id = ? AND rad_id IS NULL
                """);

            int batchLink = 0;
            int batchRad  = 0;

            for (Map.Entry<String, List<String>> entry : kanjiToComps.entrySet()) {
                Integer kanjiId = kanjiToId.get(entry.getKey());
                if (kanjiId == null) continue;

                short   position    = 0;
                boolean firstRadSet = false;

                for (String comp : entry.getValue()) {
                    Integer radId = componentToId.get(comp);
                    if (radId == null) continue;

                    insertLink.setInt(1, kanjiId);
                    insertLink.setInt(2, radId);
                    insertLink.setShort(3, position);
                    insertLink.addBatch();
                    links++;
                    position++;

                    if (!firstRadSet) {
                        updateRadId.setInt(1, radId);
                        updateRadId.setInt(2, kanjiId);
                        updateRadId.addBatch();
                        firstRadSet = true;
                        batchRad++;
                    }

                    if (++batchLink % BATCH_SIZE == 0) insertLink.executeBatch();
                    if (batchRad   % BATCH_SIZE == 0) {
                        int[] res = updateRadId.executeBatch();
                        for (int r : res) if (r > 0) updated++;
                        batchRad = 0;
                    }
                }
            }

            // Flush finaux
            if (batchLink % BATCH_SIZE != 0) insertLink.executeBatch();
            if (batchRad > 0) {
                int[] res = updateRadId.executeBatch();
                for (int r : res) if (r > 0) updated++;
            }

            insertLink.close();
            updateRadId.close();
        }

        return new int[]{links, updated};
    }

    // ── Charger kanji_character → kanji_id ───────────────────────────────

    private Map<String, Integer> loadKanjiMap() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT kanji_id, kanji_character FROM kanji WHERE kanji_character IS NOT NULL");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("kanji_character"), rs.getInt("kanji_id"));
            }
        }
        return map;
    }
}