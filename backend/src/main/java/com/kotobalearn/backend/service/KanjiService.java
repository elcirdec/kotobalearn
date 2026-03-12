package com.kotobalearn.backend.service;

import com.kotobalearn.backend.dto.*;
import com.kotobalearn.backend.model.*;
import com.kotobalearn.backend.repository.KanjiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KanjiService {

    private final KanjiRepository kanjiRepository;

    public List<KanjiSummaryDto> findAll() {
        return kanjiRepository.findAll()
                .stream()
                .map(this::toSummaryDto)
                .toList();
    }

    public List<KanjiSummaryDto> findByJlpt(String jlptCode) {
        return kanjiRepository.findByJlptLevel_JlptCode(jlptCode)
                .stream()
                .map(this::toSummaryDto)
                .toList();
    }

    public List<KanjiSummaryDto> findByGrade(Integer grade) {
        return kanjiRepository.findByKanjiGrade(grade)
                .stream()
                .map(this::toSummaryDto)
                .toList();
    }

    public KanjiDetailDto findById(Integer id) {
        Kanji kanji = kanjiRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Kanji not found: " + id));
        return toDetailDto(kanji);
    }

    public KanjiDetailDto findByCharacter(String character) {
        Kanji kanji = kanjiRepository.findByKanjiCharacter(character)
                .orElseThrow(() -> new NoSuchElementException("Kanji not found: " + character));
        return toDetailDto(kanji);
    }

    // ---- Mappers privés (Entity → DTO) ----

    private KanjiSummaryDto toSummaryDto(Kanji k) {
        KanjiSummaryDto dto = new KanjiSummaryDto();
        dto.setKanjiId(k.getKanjiId());
        dto.setKanjiCharacter(k.getKanjiCharacter());
        dto.setKanjiMeaningEnglish(k.getKanjiMeaningEnglish());
        dto.setKanjiStrokes(k.getKanjiStrokes());
        dto.setKanjiGrade(k.getKanjiGrade());
        if (k.getJlptLevel() != null) {
            dto.setJlptCode(k.getJlptLevel().getJlptCode());
        }
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
        if (k.getJlptLevel() != null) {
            dto.setJlptCode(k.getJlptLevel().getJlptCode());
        }
        if (k.getRadical() != null) {
            dto.setRadCharacter(k.getRadical().getRadCharacter());
            dto.setRadNameRomaji(k.getRadical().getRadNameRomaji());
            dto.setRadMeaningEnglish(k.getRadical().getRadMeaningEnglish());
        }
        if (k.getReadings() != null) {
            dto.setReadings(k.getReadings().stream().map(r -> {
                ReadingDto rdto = new ReadingDto();
                rdto.setReadId(r.getReadId());
                rdto.setReadType(r.getReadType());
                rdto.setReadKana(r.getReadKana());
                rdto.setReadRomaji(r.getReadRomaji());
                return rdto;
            }).toList());
        }
        if (k.getExamples() != null) {
            dto.setExamples(k.getExamples().stream().map(e -> {
                ExampleDto edto = new ExampleDto();
                edto.setExId(e.getExId());
                edto.setExJapanese(e.getExJapanese());
                edto.setExMeaningEnglish(e.getExMeaningEnglish());
                edto.setExAudioMp3Url(e.getExAudioMp3Url());
                return edto;
            }).toList());
        }
        return dto;
    }
}