package com.kotobalearn.backend.importer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class JMdictImportService {

    private final JdbcTemplate jdbcTemplate;

    public String importWords() {
        log.info("=== JMdict import started ===");

        // Augmentation de la limite d'expansion d'entités XML
        System.setProperty("jdk.xml.entityExpansionLimit", "0");
        System.setProperty("jdk.xml.totalEntitySizeLimit", "0");
        
        // 1. Précharger la map kanji (caractère → kanji_id)
        Map<String, Integer> kanjiMap = jdbcTemplate.query(
            "SELECT kanji_id, kanji_character FROM kanji WHERE kanji_character IS NOT NULL",
            rs -> {
                Map<String, Integer> map = new HashMap<>();
                while (rs.next()) {
                    map.put(rs.getString("kanji_character"), rs.getInt("kanji_id"));
                }
                return map;
            }
        );
        log.info("Kanji préchargés : {}", kanjiMap.size());

        // 2. Parse SAX + insertion
        int[] stats = {0, 0}; // [mots, exemples]

        try {
            InputStream is = getClass().getResourceAsStream("/data/JMdict_e_examp.xml");
            if (is == null) {
                return "ERROR: JMdict_e_examp.xml introuvable dans resources/data/";
            }

            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            SAXParser saxParser = factory.newSAXParser();

            JMdictHandler handler = new JMdictHandler(
                jdbcTemplate, kanjiMap, stats
            );

            saxParser.parse(is, handler);

        } catch (Exception e) {
            log.error("Erreur pendant l'import JMdict", e);
            return "ERROR: " + e.getMessage();
        }

        String result = String.format(
            "JMdict import terminé — Mots: %d | Exemples: %d",
            stats[0], stats[1]
        );
        log.info(result);
        return result;
    }

    // ─── Handler SAX ─────────────────────────────────────────────────────────

    private static class JMdictHandler extends DefaultHandler {

        private final JdbcTemplate          jdbcTemplate;
        private final Map<String, Integer>  kanjiMap;
        private final int[]                 stats;

        // État courant
        private boolean inKEle    = false;
        private boolean inREle    = false;
        private boolean inSense   = false;
        private String  glossLang = "eng";
        private String  sentLang  = "eng";

        private final StringBuilder buf = new StringBuilder();

        // Données de l'entrée en cours
        private final List<String>   kebList     = new ArrayList<>();
        private final List<String>   rebList     = new ArrayList<>();
        private final List<String>   glosses     = new ArrayList<>();
        private final List<String[]> exampleList = new ArrayList<>(); // [srce, form, jpn, eng]

        // Exemple en cours de construction
        private String exSrce, exForm, exJpn, exEng;

        // Listes de batch pour word_kanji et exemples
        private final List<Object[]> batchWordKanji = new ArrayList<>();
        private final List<Object[]> batchExamples  = new ArrayList<>();
        private static final int FLUSH_SIZE = 500;

        JMdictHandler(JdbcTemplate jdbcTemplate, Map<String, Integer> kanjiMap, int[] stats) {
            this.jdbcTemplate = jdbcTemplate;
            this.kanjiMap     = kanjiMap;
            this.stats        = stats;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            buf.setLength(0);
            switch (qName) {
                case "k_ele"   -> inKEle    = true;
                case "r_ele"   -> inREle    = true;
                case "sense"   -> inSense   = true;
                case "gloss"   -> {
                    glossLang = attrs.getValue("xml:lang");
                    if (glossLang == null) glossLang = "eng";
                }
                case "ex_sent" -> {
                    sentLang = attrs.getValue("xml:lang");
                    if (sentLang == null) sentLang = "eng";
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String text = buf.toString().trim();
            switch (qName) {
                case "entry"   -> { processEntry(); resetEntry(); }
                case "k_ele"   -> inKEle    = false;
                case "r_ele"   -> inREle    = false;
                case "sense"   -> inSense   = false;
                case "keb"     -> { if (inKEle  && !text.isEmpty()) kebList.add(text); }
                case "reb"     -> { if (inREle  && !text.isEmpty()) rebList.add(text); }
                case "gloss"   -> {
                    if (inSense && "eng".equals(glossLang) && !text.isEmpty())
                        glosses.add(text);
                }
                case "ex_srce" -> exSrce = text;
                case "ex_text" -> exForm  = text;
                case "ex_sent" -> {
                    if ("jpn".equals(sentLang)) exJpn = text;
                    else if ("eng".equals(sentLang)) exEng = text;
                }
                case "example" -> {
                    if (exJpn != null && exEng != null)
                        exampleList.add(new String[]{exSrce, exForm, exJpn, exEng});
                    exSrce = null; exForm = null; exJpn = null; exEng = null;
                }
            }
            buf.setLength(0);
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            buf.append(ch, start, length);
        }

        // ─── Traitement d'une entrée complète ────────────────────────────────

        private void processEntry() {
            if (rebList.isEmpty() && kebList.isEmpty()) return;
            if (glosses.isEmpty()) return; // pas de traduction EN → on ignore

            String wordJapanese = kebList.isEmpty() ? rebList.get(0) : kebList.get(0);
            String wordReading  = rebList.isEmpty()  ? wordJapanese  : rebList.get(0);
            String translation  = String.join("; ", glosses);

            // Insérer le mot et récupérer l'ID généré
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO word (word_japanese, word_pronunciation_hiragana, word_romaji, word_translation_en) VALUES (?, ?, ?, ?)",
                    new String[]{"word_id"}
                );
                ps.setString(1, wordJapanese);
                ps.setString(2, wordReading);
                ps.setString(3, ""); // romaji vide pour l'instant (colonne NOT NULL)
                ps.setString(4, translation);
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key == null) return;
            int wordId = key.intValue();
            stats[0]++;

            // Préparer les liens word_kanji (déduplication par Set)
            Set<Integer> seenKanji = new HashSet<>();
            for (String keb : kebList) {
                int i = 0;
                while (i < keb.length()) {
                    int cp = keb.codePointAt(i);
                    String ch = new String(Character.toChars(cp));
                    Integer kanjiId = kanjiMap.get(ch);
                    if (kanjiId != null && seenKanji.add(kanjiId)) {
                        batchWordKanji.add(new Object[]{wordId, kanjiId});
                    }
                    i += Character.charCount(cp);
                }
            }

            // Préparer les exemples
            for (String[] ex : exampleList) {
                batchExamples.add(new Object[]{wordId, ex[2], ex[3], ex[0], ex[1]});
            }

            // Flush tous les 500 mots
            if (stats[0] % FLUSH_SIZE == 0) {
                flushBatches();
                log.info("Progression : {} mots importés", stats[0]);
            }
        }

        // ─── Flush des batches ───────────────────────────────────────────────

        private void flushBatches() {
            if (!batchWordKanji.isEmpty()) {
                jdbcTemplate.batchUpdate(
                    "INSERT INTO word_kanji (word_id, kanji_id) VALUES (?, ?)",
                    batchWordKanji
                );
                batchWordKanji.clear();
            }
            if (!batchExamples.isEmpty()) {
                jdbcTemplate.batchUpdate(
                    "INSERT INTO word_example (word_id, we_japanese, we_english, we_tatoeba_id, we_form) VALUES (?, ?, ?, ?, ?)",
                    batchExamples
                );
                stats[1] += batchExamples.size();
                batchExamples.clear();
            }
        }

        // Flush final à la fin du fichier
        @Override
        public void endDocument() {
            flushBatches();
        }

        private void resetEntry() {
            kebList.clear();
            rebList.clear();
            glosses.clear();
            exampleList.clear();
        }

        // Empêche le chargement du DTD externe (le DTD interne est géré nativement)
        @Override
        public InputSource resolveEntity(String publicId, String systemId) {
            return new InputSource(new StringReader(""));
        }
    }
}