package com.kotobalearn.backend.importer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kotobalearn.backend.model.*;
import com.kotobalearn.backend.repository.*;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KanjiAliveImportService {

    private final RadicalRepository      radicalRepository;
    private final KanjiRepository        kanjiRepository;
    private final ReadingRepository      readingRepository;
    private final ExampleRepository      exampleRepository;
    private final ObjectMapper           objectMapper = new ObjectMapper();

    // Base URL des médias KanjiAlive (CDN public, CC BY)
    private static final String MEDIA_BASE     = "https://media.kanjialive.com";
    private static final String RADICAL_IMG    = MEDIA_BASE + "/radical_character/";
    private static final String RAD_FRAMES     = MEDIA_BASE + "/rad_frames/";
    private static final String RAD_POSITIONS  = MEDIA_BASE + "/rad_positions/";
    private static final String KANJI_STROKES  = MEDIA_BASE + "/kanji_strokes/";
    private static final String KANJI_MP4      = MEDIA_BASE + "/kanji_animations/kanji_mp4/";
    private static final String KANJI_WEBM     = MEDIA_BASE + "/kanji_animations/kanji_webm/";

    // ----------------------------------------------------------------
    //  POINT D'ENTRÉE PRINCIPAL
    // ----------------------------------------------------------------

    @Transactional
    public void importAll() throws IOException, CsvValidationException {
        log.info("=== KanjiAlive import started ===");
        Map<String, Radical> radicalMap = importRadicals();
        importKanji(radicalMap);
        log.info("=== KanjiAlive import finished ===");
    }

    // ----------------------------------------------------------------
    //  1. IMPORT DES RADICAUX (japanese-radicals.csv)
    //
    //  Colonnes :
    //  0  Radical ID#
    //  1  Stroke#
    //  2  Radical (caractère)
    //  3  Meaning
    //  4  Reading-J  (hiragana)
    //  5  Reading-R  (romaji)
    //  6  R-Filename (SVG du radical)
    //  7  Anim-Filename (base du nom des frames SVG)
    //  8  Position-J (hiragana)
    //  9  Position-R (romaji)  — peut être absent
    // ----------------------------------------------------------------

    private Map<String, Radical> importRadicals() throws IOException, CsvValidationException {

        Map<String, Radical> map = new HashMap<>();

        try (CSVReader reader = csvReader("data/japanese-radicals.csv")) {

            reader.readNext(); // skip header

            String[] row;
            int count = 0;

            while ((row = reader.readNext()) != null) {

                if (row.length < 6) continue; // ligne incomplète

                String character  = clean(row[2]);
                if (character.isEmpty()) continue;

                // Évite les doublons si on relance l'import
                if (radicalRepository.findByRadCharacter(character).isPresent()) continue;

                Radical radical = new Radical();
                radical.setRadCharacter(character);
                radical.setRadStrokes(parseIntSafe(row[1]));
                radical.setRadMeaningEnglish(clean(row[3]));
                radical.setRadNameHiragana(clean(row[4]));
                radical.setRadNameRomaji(clean(row[5]));

                // SVG du radical
                String rFilename = row.length > 6 ? clean(row[6]) : "";
                if (!rFilename.isEmpty()) {
                    radical.setRadImageUrl(RADICAL_IMG + rFilename + ".svg");
                }

                // Frames d'animation (3 frames : base0.svg, base1.svg, base2.svg)
                String animFilename = row.length > 7 ? clean(row[7]) : "";

                // Position
                if (row.length > 8) radical.setRadPositionHiragana(clean(row[8]));
                if (row.length > 9) radical.setRadPositionRomaji(clean(row[9]));

                // Icône de position
                String posRomaji = radical.getRadPositionRomaji();
                if (posRomaji != null && !posRomaji.isEmpty()) {
                    radical.setRadPositionIconUrl(RAD_POSITIONS + posRomaji + ".svg");
                }

                radical = radicalRepository.save(radical);

                // Frames d'animation (entités RadicalAnimation)
                if (!animFilename.isEmpty()) {
                    List<RadicalAnimation> frames = new ArrayList<>();
                    for (int i = 0; i <= 2; i++) {
                        RadicalAnimation frame = new RadicalAnimation();
                        frame.setRadical(radical);
                        frame.setRaOrder(i);
                        frame.setRaImageUrl(RAD_FRAMES + animFilename + i + ".svg");
                        frames.add(frame);
                    }
                    radical.setAnimations(frames);
                    radicalRepository.save(radical);
                }

                map.put(character, radical);
                count++;
            }

            log.info("Radicals imported: {}", count);
        }

        return map;
    }

    // ----------------------------------------------------------------
    //  2. IMPORT DES KANJI (ka_data.csv)
    //
    //  Colonnes :
    //  0  kanji           — caractère
    //  1  kname           — clé fichiers médias
    //  2  kstroke         — nombre de traits
    //  3  kmeaning        — sens anglais
    //  4  kgrade          — grade scolaire
    //  5  kunyomi_ja      — kun'yomi hiragana (peut être vide ou "n/a")
    //  6  kunyomi         — kun'yomi romaji
    //  7  onyomi_ja       — on'yomi katakana
    //  8  onyomi          — on'yomi romaji
    //  9  examples        — JSON [[japanese, meaning], ...]
    //  10 radical         — caractère du radical
    //  11 rad_order
    //  12 rad_stroke
    //  13 rad_name_ja
    //  14 rad_name
    //  15 rad_meaning
    //  16 rad_position_ja
    //  17 rad_position
    // ----------------------------------------------------------------

    private void importKanji(Map<String, Radical> radicalMap)
            throws IOException, CsvValidationException {

        try (CSVReader reader = csvReader("data/ka_data.csv")) {

            reader.readNext(); // skip header

            String[] row;
            int countKanji = 0;
            int countSkipped = 0;

            while ((row = reader.readNext()) != null) {

                if (row.length < 4) continue;

                String character = clean(row[0]);
                if (character.isEmpty()) continue;

                // Évite les doublons
                if (kanjiRepository.findByKanjiCharacter(character).isPresent()) {
                    countSkipped++;
                    continue;
                }

                String kname   = row.length > 1  ? clean(row[1])  : "";
                int    strokes = row.length > 2  ? parseIntSafe(row[2]) : 0;
                String meaning = row.length > 3  ? clean(row[3])  : "";
                int    grade   = row.length > 4  ? parseIntSafe(row[4]) : 0;

                // --- Kanji entity ---
                Kanji kanji = new Kanji();
                kanji.setKanjiCharacter(character);
                kanji.setKanjiMeaningEnglish(meaning);
                kanji.setKanjiStrokes(strokes);
                if (grade > 0) kanji.setKanjiGrade(grade);

                // URLs médias construites depuis kname
                if (!kname.isEmpty()) {
                    kanji.setKanjiVideoPosterUrl(
                        KANJI_STROKES + kname + "_" + strokes + ".svg");
                    kanji.setKanjiVideoMp4Url(
                        KANJI_MP4 + kname + "_00.mp4");
                    kanji.setKanjiVideoWebmUrl(
                        KANJI_WEBM + kname + "_00.webm");
                }

                // Radical : on cherche d'abord dans le map chargé
                String radChar = row.length > 10 ? clean(row[10]) : "";
                if (!radChar.isEmpty()) {
                    Radical rad = radicalMap.get(radChar);
                    if (rad == null) {
                        // radical absent du CSV radicaux → on le crée à la volée
                        rad = createRadicalFromKanjiRow(row, radChar);
                        radicalMap.put(radChar, rad);
                    }
                    kanji.setRadical(rad);
                }

                kanji = kanjiRepository.save(kanji);

                // --- Lectures ---
                saveReadings(kanji,
                    row.length > 7 ? clean(row[7]) : "",   // onyomi_ja
                    row.length > 8 ? clean(row[8]) : "",   // onyomi romaji
                    row.length > 5 ? clean(row[5]) : "",   // kunyomi_ja
                    row.length > 6 ? clean(row[6]) : ""    // kunyomi romaji
                );

                // --- Exemples ---
                if (row.length > 9 && !clean(row[9]).isEmpty()) {
                    saveExamples(kanji, clean(row[9]));
                }

                countKanji++;
                if (countKanji % 100 == 0) {
                    log.info("  {} kanji imported...", countKanji);
                }
            }

            log.info("Kanji imported: {} | Skipped (already exist): {}",
                     countKanji, countSkipped);
        }
    }

    // ----------------------------------------------------------------
    //  LECTURES  (on'yomi et kun'yomi peuvent être multiples,
    //             séparés par des virgules ou des 、)
    // ----------------------------------------------------------------

    private void saveReadings(Kanji kanji,
                               String onyomiJa, String onyomiRomaji,
                               String kunyomiJa, String kunyomiRomaji) {

        // On'yomi
        List<String> onJaList  = splitReadings(onyomiJa);
        List<String> onRoList  = splitReadings(onyomiRomaji);
        for (int i = 0; i < onJaList.size(); i++) {
            String kana   = onJaList.get(i).trim();
            String romaji = i < onRoList.size() ? onRoList.get(i).trim() : "";
            if (kana.isEmpty() || kana.equalsIgnoreCase("n/a")) continue;
            Reading r = new Reading();
            r.setKanji(kanji);
            r.setReadType("ON");
            r.setReadKana(kana);
            r.setReadRomaji(romaji);
            readingRepository.save(r);
        }

        // Kun'yomi
        List<String> kunJaList = splitReadings(kunyomiJa);
        List<String> kunRoList = splitReadings(kunyomiRomaji);
        for (int i = 0; i < kunJaList.size(); i++) {
            String kana   = kunJaList.get(i).trim();
            String romaji = i < kunRoList.size() ? kunRoList.get(i).trim() : "";
            if (kana.isEmpty() || kana.equalsIgnoreCase("n/a")) continue;
            Reading r = new Reading();
            r.setKanji(kanji);
            r.setReadType("KUN");
            r.setReadKana(kana);
            r.setReadRomaji(romaji);
            readingRepository.save(r);
        }
    }

    // ----------------------------------------------------------------
    //  EXEMPLES  (JSON dans le CSV : [["word", "meaning"], ...])
    // ----------------------------------------------------------------

    private void saveExamples(Kanji kanji, String json) {
        try {
            // Le JSON peut contenir des guillemets doublés — on nettoie
            String cleanJson = json.replace("\"\"", "\"");

            List<List<String>> pairs = objectMapper.readValue(
                cleanJson, new TypeReference<>() {});

            for (List<String> pair : pairs) {
                if (pair.size() < 2) continue;
                Example ex = new Example();
                ex.setKanji(kanji);
                ex.setExJapanese(pair.get(0));
                ex.setExMeaningEnglish(pair.get(1));
                // Audio : non disponible dans les CSV → null (ajout futur via API)
                exampleRepository.save(ex);
            }
        } catch (Exception e) {
            log.warn("Could not parse examples for kanji {}: {}",
                     kanji.getKanjiCharacter(), e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    //  RADICAL À LA VOLÉE  (si absent du CSV des radicaux)
    // ----------------------------------------------------------------

    private Radical createRadicalFromKanjiRow(String[] row, String radChar) {
        Radical rad = new Radical();
        rad.setRadCharacter(radChar);
        rad.setRadStrokes(row.length > 12 ? parseIntSafe(row[12]) : 0);
        rad.setRadNameHiragana(row.length > 13 ? clean(row[13]) : "");
        rad.setRadNameRomaji(row.length > 14 ? clean(row[14]) : "");
        rad.setRadMeaningEnglish(row.length > 15 ? clean(row[15]) : "");
        rad.setRadPositionHiragana(row.length > 16 ? clean(row[16]) : null);
        rad.setRadPositionRomaji(row.length > 17 ? clean(row[17]) : null);
        String pos = rad.getRadPositionRomaji();
        if (pos != null && !pos.isEmpty()) {
            rad.setRadPositionIconUrl(RAD_POSITIONS + pos + ".svg");
        }
        return radicalRepository.save(rad);
    }

    // ----------------------------------------------------------------
    //  UTILITAIRES
    // ----------------------------------------------------------------

    private CSVReader csvReader(String classpath) throws IOException {
        return new CSVReader(new InputStreamReader(
            new ClassPathResource(classpath).getInputStream(),
            StandardCharsets.UTF_8));
    }

    private String clean(String s) {
        return s == null ? "" : s.trim().replace("\uFEFF", ""); // retire BOM UTF-8
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(clean(s)); }
        catch (NumberFormatException e) { return 0; }
    }

    private List<String> splitReadings(String s) {
        if (s == null || s.isBlank() || s.equalsIgnoreCase("n/a")) {
            return Collections.emptyList();
        }
        // Sépare sur virgule ou 読点 japonais （、）
        return Arrays.asList(s.split("[,、]"));
    }
}