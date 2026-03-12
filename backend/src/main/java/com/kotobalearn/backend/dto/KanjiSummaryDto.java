// Utilisé dans les listes (pas toutes les infos, juste l'essentiel)
package com.kotobalearn.backend.dto;

import lombok.Data;

@Data
public class KanjiSummaryDto {
    private Integer kanjiId;
    private String kanjiCharacter;
    private String kanjiMeaningEnglish;
    private Integer kanjiStrokes;
    private Integer kanjiGrade;
    private String jlptCode;           // ex: "N5"
}