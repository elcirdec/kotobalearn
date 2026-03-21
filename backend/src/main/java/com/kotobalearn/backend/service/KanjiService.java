package com.kotobalearn.backend.service;

import com.kotobalearn.backend.dto.ExampleDto;
import com.kotobalearn.backend.dto.KanjiDetailDto;
import com.kotobalearn.backend.dto.KanjiDetailDto.ComponentDto;
import com.kotobalearn.backend.dto.KanjiSummaryDto;
import com.kotobalearn.backend.dto.ReadingDto;
import com.kotobalearn.backend.model.Kanji;
import com.kotobalearn.backend.model.KanjiComponent;
import com.kotobalearn.backend.repository.KanjiComponentRepository;
import com.kotobalearn.backend.repository.KanjiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KanjiService {

    private final KanjiRepository          kanjiRepository;
    private final KanjiComponentRepository componentRepository;

    public List<KanjiSummaryDto> findByCriteria(
        String jlpt, Integer grade, Integer strokes,
        List<Integer> radicalIds, String search
    ) {
        boolean hasJlpt     = jlpt       != null && !jlpt.isBlank();
        boolean hasGrade    = grade      != null;
        boolean hasStrokes  = strokes    != null;
        boolean hasRadicals = radicalIds != null && !radicalIds.isEmpty();
        boolean hasSearch   = search     != null && !search.isBlank();

        List<Integer> safeRadicalIds = radicalIds == null ? List.of() : radicalIds;
        String safeSearch = search == null ? "" : search;
        String searchLower = safeSearch.toLowerCase();

        List<Kanji> base;

        if (hasRadicals) {
            base = kanjiRepository.findByAllComponents(radicalIds, (long) safeRadicalIds.size());
        } else if (hasStrokes && hasJlpt && hasGrade) {
            base = kanjiRepository.findByStrokesJlptGrade(strokes, jlpt, grade);
        } else if (hasStrokes && hasJlpt) {
            base = kanjiRepository.findByStrokesAndJlpt(strokes, jlpt);
        } else if (hasStrokes && hasGrade) {
            base = kanjiRepository.findByStrokesAndGrade(strokes, grade);
        } else if (hasStrokes) {
            base = kanjiRepository.findByKanjiStrokes(strokes);
        } else if (hasJlpt && hasGrade) {
            base = kanjiRepository.findByJlptAndGrade(jlpt, grade);
        } else if (hasJlpt) {
            base = kanjiRepository.findByJlptLevel_JlptCode(jlpt);
        } else if (hasGrade) {
            base = kanjiRepository.findByKanjiGrade(grade);
        } else {
            base = hasSearch ? List.of() : kanjiRepository.findAll();
        }

        Stream<Kanji> stream = base.stream();

        if (hasRadicals && (hasJlpt || hasGrade || hasStrokes)) {
            stream = applyExtraFilters(stream, jlpt, hasJlpt, grade, hasGrade, strokes, hasStrokes);
        }

        if (hasSearch) {
            String q = searchLower;

            if (!base.isEmpty() || hasRadicals || hasStrokes || hasJlpt || hasGrade) {
                stream = stream.filter(k ->
                    k.getKanjiMeaningEnglish() != null
                    && k.getKanjiMeaningEnglish().toLowerCase().contains(q)
                );
            } else {
                stream = kanjiRepository
                    .findByKanjiMeaningEnglishContainingIgnoreCase(search)
                    .stream();
            }

            return stream
                .sorted(
                    Comparator.comparingInt((Kanji k) -> rankMeaning(k.getKanjiMeaningEnglish(), q))
                    .thenComparingInt(k -> k.getKanjiMeaningEnglish() == null
                        ? 999 : k.getKanjiMeaningEnglish().length())
                )
                .map(this::toSummaryDto)
                .toList();
        }

        return stream.map(this::toSummaryDto).toList();
    }

    public KanjiDetailDto findById(Integer id) {
        return toDetailDto(kanjiRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Kanji not found: " + id)));
    }

    public KanjiDetailDto findByCharacter(String character) {
        return toDetailDto(kanjiRepository.findByKanjiCharacter(character)
            .orElseThrow(() -> new NoSuchElementException("Kanji not found: " + character)));
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private int rankMeaning(String meaning, String q) {
        if (meaning == null) return 4;
        String m = meaning.toLowerCase();
        if (m.equals(q))     return 1;
        if (m.startsWith(q)) return 2;
        return 3;
    }

    private Stream<Kanji> applyExtraFilters(
    Stream<Kanji> stream,
    String jlpt, boolean hasJlpt,
    Integer grade, boolean hasGrade,
    Integer strokes, boolean hasStrokes
) {
    return stream.filter(k -> {
        boolean ok = true;
        if (hasJlpt) {
            ok = ok && (k.getJlptLevel() != null && jlpt.equals(k.getJlptLevel().getJlptCode()));
        }
        if (hasGrade) {
            ok = ok && (k.getKanjiGrade() != null && grade.equals(k.getKanjiGrade()));
        }
        if (hasStrokes) {
            ok = ok && strokes.equals(k.getKanjiStrokes()); 
        }
        return ok;
    });
}

    // ── Mappers ────────────────────────────────────────────────────────────

    private KanjiSummaryDto toSummaryDto(Kanji k) {
        KanjiSummaryDto dto = new KanjiSummaryDto();
        dto.setKanjiId(k.getKanjiId());
        dto.setKanjiCharacter(k.getKanjiCharacter());
        dto.setKanjiMeaningEnglish(k.getKanjiMeaningEnglish());
        dto.setKanjiStrokes(k.getKanjiStrokes());
        dto.setKanjiGrade(k.getKanjiGrade());
        if (k.getJlptLevel() != null) dto.setJlptCode(k.getJlptLevel().getJlptCode());
        return dto;
    }

    private KanjiDetailDto toDetailDto(Kanji k) {
        KanjiDetailDto dto = new KanjiDetailDto();
        dto.setKanjiId(k.getKanjiId());
        dto.setKanjiCharacter(k.getKanjiCharacter());
        dto.setKanjiMeaningEnglish(k.getKanjiMeaningEnglish());
        dto.setKanjiStrokes(k.getKanjiStrokes());
        dto.setKanjiGrade(k.getKanjiGrade());
        dto.setKanjiVideoPosterUrl(k.getKanjiVideoPosterUrl());
        dto.setKanjiVideoMp4Url(k.getKanjiVideoMp4Url());
        dto.setKanjiVideoWebmUrl(k.getKanjiVideoWebmUrl());
        if (k.getJlptLevel() != null) dto.setJlptCode(k.getJlptLevel().getJlptCode());

        // Composants visuels (KRADFILE)
        List<KanjiComponent> comps = componentRepository.findByKanjiId(k.getKanjiId());
        dto.setComponents(comps.stream().map(kc -> {
            ComponentDto c = new ComponentDto();
            c.setRadId(kc.getRadical().getRadId());
            c.setRadCharacter(kc.getRadical().getRadCharacter());
            c.setRadNameRomaji(kc.getRadical().getRadNameRomaji());
            c.setRadMeaningEnglish(kc.getRadical().getRadMeaningEnglish());
            c.setRadStrokes(kc.getRadical().getRadStrokes());
            c.setPosition(kc.getKcPosition());
            return c;
        }).toList());

        // Lectures
        if (k.getReadings() != null) {
            dto.setReadings(k.getReadings().stream().map(r -> {
                ReadingDto rd = new ReadingDto();
                rd.setReadId(r.getReadId()); rd.setReadType(r.getReadType());
                rd.setReadKana(r.getReadKana()); rd.setReadRomaji(r.getReadRomaji());
                return rd;
            }).toList());
        }

        // Exemples
        if (k.getExamples() != null) {
            dto.setExamples(k.getExamples().stream().map(e -> {
                ExampleDto ed = new ExampleDto();
                ed.setExId(e.getExId()); ed.setExJapanese(e.getExJapanese());
                ed.setExMeaningEnglish(e.getExMeaningEnglish());
                ed.setExAudioMp3Url(e.getExAudioMp3Url());
                return ed;
            }).toList());
        }

        return dto;
    }
}