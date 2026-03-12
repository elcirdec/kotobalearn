package com.kotobalearn.backend.importer;

import com.kotobalearn.backend.model.JlptLevel;
import com.kotobalearn.backend.model.Kanji;
import com.kotobalearn.backend.model.Reading;
import com.kotobalearn.backend.repository.JlptLevelRepository;
import com.kotobalearn.backend.repository.KanjiRepository;
import com.kotobalearn.backend.repository.ReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KanjidicImportService {

    private final KanjiRepository    kanjiRepository;
    private final JlptLevelRepository jlptLevelRepository;
    private final ReadingRepository  readingRepository;

    // Mapping ancien système JLPT (1-4) → nouveau code (N2-N5)
    // Kanjidic2 ne couvre pas N1 : son niveau "1" correspond à N2
    private static final Map<String, String> JLPT_MAPPING = new HashMap<>();
    static {
        JLPT_MAPPING.put("4", "N5");
        JLPT_MAPPING.put("3", "N4");
        JLPT_MAPPING.put("2", "N3");
        JLPT_MAPPING.put("1", "N2");
    }

    @Transactional
    public String importAllKanji() {
        log.info("=== Kanjidic2 full import started ===");

        // Charger les JlptLevel en mémoire
        Map<String, JlptLevel> jlptMap = new HashMap<>();
        jlptLevelRepository.findAll().forEach(j -> jlptMap.put(j.getJlptCode(), j));

        int updated  = 0; // kanji déjà en DB (KanjiAlive) → JLPT mis à jour
        int created  = 0; // kanji nouveaux → créés avec données Kanjidic2
        int skipped  = 0; // kanji sans données utilisables

        try {
            InputStream is = getClass().getResourceAsStream("/data/kanjidic2.xml");
            if (is == null) {
                return "ERROR: kanjidic2.xml not found in resources/data/";
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);

            NodeList characters = doc.getElementsByTagName("character");
            log.info("Kanjidic2: {} characters to process", characters.getLength());

            for (int i = 0; i < characters.getLength(); i++) {
                Element character = (Element) characters.item(i);

                String literal = getTextContent(character, "literal");
                if (literal == null || literal.isEmpty()) { skipped++; continue; }

                // --- JLPT (peut être null, c'est ok) ---
                JlptLevel jlptLevel = null;
                String jlptRaw = getTextContent(character, "jlpt");
                if (jlptRaw != null && !jlptRaw.isEmpty()) {
                    String code = JLPT_MAPPING.get(jlptRaw);
                    if (code != null) jlptLevel = jlptMap.get(code);
                }

                Optional<Kanji> existing = kanjiRepository.findByKanjiCharacter(literal);

                if (existing.isPresent()) {
                    // ── Kanji KanjiAlive → on met seulement le JLPT à jour ──
                    if (jlptLevel != null) {
                        Kanji k = existing.get();
                        k.setJlptLevel(jlptLevel);
                        kanjiRepository.save(k);
                    }
                    updated++;

                } else {
                    // ── Kanji inconnu → on crée une fiche légère depuis Kanjidic2 ──
                    String meaning = buildMeaning(character);
                    if (meaning == null || meaning.isEmpty()) { skipped++; continue; }

                    Integer strokes = parseInteger(getTextContent(character, "stroke_count"));
                    Integer grade   = parseInteger(getTextContent(character, "grade"));

                    Kanji kanji = new Kanji();
                    kanji.setKanjiCharacter(literal);
                    kanji.setKanjiMeaningEnglish(meaning);
                    kanji.setKanjiStrokes(strokes);
                    kanji.setKanjiGrade(grade);
                    kanji.setJlptLevel(jlptLevel);
                    // Pas de médias (KanjiAlive only) — colonnes nullable
                    kanji.setKanjiVideoPosterUrl(null);
                    kanji.setKanjiVideoMp4Url(null);
                    kanji.setKanjiVideoWebmUrl(null);

                    kanjiRepository.save(kanji);

                    // Lectures ON et KUN (sans romaji — null accepté)
                    saveReadings(character, kanji, "ja_on",  "ON");
                    saveReadings(character, kanji, "ja_kun", "KUN");

                    created++;
                }

                // Log de progression toutes les 1000 entrées
                if ((i + 1) % 1000 == 0) {
                    log.info("Progress: {}/{} — created={} updated={} skipped={}",
                            i + 1, characters.getLength(), created, updated, skipped);
                }
            }

        } catch (Exception e) {
            log.error("Error during Kanjidic2 import", e);
            return "ERROR: " + e.getMessage();
        }

        String result = String.format(
            "Kanjidic2 import finished — Created: %d | Updated (JLPT): %d | Skipped: %d | Total DB: ~%d",
            created, updated, skipped, 1235 + created
        );
        log.info(result);
        return result;
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    /**
     * Construit la signification anglaise en joignant tous les <meaning> sans attribut lang.
     * (Avec lang="fr", "es"... → ignorés ici)
     */
    private String buildMeaning(Element character) {
        NodeList meanings = character.getElementsByTagName("meaning");
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < meanings.getLength(); i++) {
            var node = (org.w3c.dom.Element) meanings.item(i);
            // Pas d'attribut m_lang → anglais
            if (!node.hasAttribute("m_lang")) {
                String text = node.getTextContent().trim();
                if (!text.isEmpty()) parts.add(text);
            }
        }
        return String.join(", ", parts);
    }

    /**
     * Sauvegarde les lectures d'un type donné (ja_on / ja_kun) pour un kanji.
     * readRomaji laissé null (pas de données romaji dans Kanjidic2).
     */
    private void saveReadings(Element character, Kanji kanji, String rType, String readType) {
        NodeList readings = character.getElementsByTagName("reading");
        for (int i = 0; i < readings.getLength(); i++) {
            var node = (org.w3c.dom.Element) readings.item(i);
            if (rType.equals(node.getAttribute("r_type"))) {
                String kana = node.getTextContent().trim();
                if (!kana.isEmpty()) {
                    Reading r = new Reading();
                    r.setKanji(kanji);
                    r.setReadType(readType);
                    r.setReadKana(kana);
                    r.setReadRomaji(null); // pas disponible dans Kanjidic2
                    readingRepository.save(r);
                }
            }
        }
    }

    private String getTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent().trim();
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isEmpty()) return null;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException e) { return null; }
    }
}