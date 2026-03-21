package com.kotobalearn.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class KanjiDetailDto {

    private Integer kanjiId;
    private String  kanjiCharacter;
    private String  kanjiMeaningEnglish;
    private Integer kanjiStrokes;
    private Integer kanjiGrade;
    private String  kanjiVideoPosterUrl;
    private String  kanjiVideoMp4Url;
    private String  kanjiVideoWebmUrl;
    private String  jlptCode;

    /** Composants visuels du kanji (depuis kanji_component / KRADFILE) */
    private List<ComponentDto> components;

    /** Lectures on'yomi et kun'yomi */
    private List<ReadingDto> readings;

    /** Exemples KanjiAlive */
    private List<ExampleDto> examples;

    // ── DTO imbriqués ──────────────────────────────────────────────────────

    @Getter @Setter
    public static class ComponentDto {
        private Integer radId;
        private String  radCharacter;
        private String  radNameRomaji;
        private String  radMeaningEnglish;
        private Integer radStrokes;
        private Short   position;
    }
}