// Utilisé pour la fiche complète d'un kanji
package com.kotobalearn.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class KanjiDetailDto {
    private Integer kanjiId;
    private String kanjiCharacter;
    private String kanjiMeaningEnglish;
    private Integer kanjiStrokes;
    private Integer kanjiGrade;
    private String jlptCode;
    private String kanjiVideoPosterUrl;
    private String kanjiVideoMp4Url;
    private String kanjiVideoWebmUrl;
    // Radical simplifié
    private String radCharacter;
    private String radNameRomaji;
    private String radMeaningEnglish;
    // Lectures et exemples
    private List<ReadingDto> readings;
    private List<ExampleDto> examples;
}