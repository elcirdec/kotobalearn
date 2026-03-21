package com.kotobalearn.backend.importer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.sql.DataSource;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

/**
 * Relit le fichier JMdict XML pour extraire :
 *  - ent_seq → word_jmdict_seq
 *  - ke_pri / re_pri → word_frequency_rank
 *
 * Fait un UPDATE des mots existants par correspondance sur word_japanese.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JMdictEnrichService {

    private final DataSource dataSource;

    private static final String JMDICT_PATH = "/data/JMdict_e_examp.xml";
    private static final int    BATCH_SIZE  = 500;

    public String enrich() throws Exception {

        // Désactiver les limites XML pour les entités
        System.setProperty("jdk.xml.entityExpansionLimit", "0");
        System.setProperty("jdk.xml.totalEntitySizeLimit", "0");
        
        log.info("Démarrage enrichissement JMdict (seq + fréquence)...");

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setValidating(false);

        List<EnrichEntry> entries = new ArrayList<>();
        int[] counts = {0, 0}; // [updatedSeq, updatedFreq]

        try (InputStream is = getClass().getResourceAsStream(JMDICT_PATH)) {
            if (is == null) throw new RuntimeException("Fichier JMdict introuvable : " + JMDICT_PATH);

            factory.newSAXParser().parse(is, new DefaultHandler() {

                int    currentSeq    = 0;
                String currentKeb    = null;
                short  frequencyRank = 0;
                boolean firstKeb     = true;
                final StringBuilder buf = new StringBuilder();

                @Override
                public void startElement(String u, String l, String qName, Attributes a) {
                    buf.setLength(0);
                    if ("entry".equals(qName)) {
                        currentSeq    = 0;
                        currentKeb    = null;
                        frequencyRank = 0;
                        firstKeb      = true;
                    }
                }

                @Override
                public void endElement(String u, String l, String qName) {
                    String val = buf.toString().trim();
                    switch (qName) {

                        case "ent_seq" ->
                            currentSeq = Integer.parseInt(val);

                        case "keb" -> {
                            // On garde uniquement la première forme écrite
                            if (firstKeb) {
                                currentKeb = val;
                                firstKeb   = false;
                            }
                        }

                        case "ke_pri", "re_pri" -> {
                            short rank = calcRank(val);
                            if (frequencyRank == 0 || rank < frequencyRank) {
                                frequencyRank = rank;
                            }
                        }

                        case "entry" -> {
                            if (currentKeb != null && currentSeq > 0) {
                                entries.add(new EnrichEntry(
                                    currentKeb,
                                    currentSeq,
                                    frequencyRank == 0 ? null : frequencyRank
                                ));
                            }
                            // Flush par batch
                            if (entries.size() >= BATCH_SIZE * 10) {
                                try {
                                    flush(entries, counts);
                                } catch (Exception e) {
                                    log.error("Erreur flush", e);
                                }
                                entries.clear();
                            }
                        }
                    }
                }

                @Override
                public void characters(char[] ch, int start, int length) {
                    buf.append(ch, start, length);
                }
            });
        }

        // Flush final
        if (!entries.isEmpty()) flush(entries, counts);

        String result = String.format(
            "Enrichissement terminé — seq: %d | fréquence: %d",
            counts[0], counts[1]
        );
        log.info(result);
        return result;
    }

    /**
     * Rang de fréquence JMdict :
     *   ichi1, news1, spec1 → 1 (très courant)
     *   ichi2, news2, spec2 → 2 (courant)
     *   gai1                → 3 (loanword courant)
     *   gai2                → 4 (loanword)
     *   autres              → 5
     */
    private short calcRank(String pri) {
        return switch (pri) {
            case "ichi1", "news1", "spec1" -> 1;
            case "ichi2", "news2", "spec2" -> 2;
            case "gai1"                    -> 3;
            case "gai2"                    -> 4;
            default                        -> 5;
        };
    }

    private void flush(List<EnrichEntry> entries, int[] counts) throws Exception {
        String sql = """
            UPDATE word
            SET word_jmdict_seq    = ?,
                word_frequency_rank = ?
            WHERE word_japanese = ?
              AND word_jmdict_seq IS NULL
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int batch = 0;
            for (EnrichEntry e : entries) {
                ps.setInt(1, e.seq());
                if (e.freqRank() != null) {
                    ps.setShort(2, e.freqRank());
                } else {
                    ps.setNull(2, java.sql.Types.SMALLINT);
                }
                ps.setString(3, e.japanese());
                ps.addBatch();

                if (++batch % BATCH_SIZE == 0) {
                    int[] res = ps.executeBatch();
                    for (int r : res) {
                        if (r > 0) {
                            counts[0]++;
                            if (e.freqRank() != null) counts[1]++;
                        }
                    }
                }
            }
            // Flush du dernier batch partiel
            int[] res = ps.executeBatch();
            for (int r : res) if (r > 0) counts[0]++;
        }
    }

    record EnrichEntry(String japanese, int seq, Short freqRank) {}
}