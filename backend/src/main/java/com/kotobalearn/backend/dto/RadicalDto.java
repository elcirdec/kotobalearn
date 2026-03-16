package com.kotobalearn.backend.dto;

import com.kotobalearn.backend.model.Radical;

public record RadicalDto(
    Integer radId,
    String  radCharacter,
    Integer radStrokes,
    String  radNameHiragana,
    String  radNameRomaji,
    String  radMeaningEnglish,
    String  radPositionRomaji
) {
    public static RadicalDto from(Radical r) {
        return new RadicalDto(
            r.getRadId(),
            r.getRadCharacter(),
            r.getRadStrokes(),
            r.getRadNameHiragana(),
            r.getRadNameRomaji(),
            r.getRadMeaningEnglish(),
            r.getRadPositionRomaji()
        );
    }
}