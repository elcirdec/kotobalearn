package com.kotobalearn.backend.dto;

import java.util.List;

public record WordDetailDto(
    Integer          wordId,
    String           wordJapanese,
    String           wordReading,
    String           wordTranslationEn,
    String           wordTranslationFr,
    String           jlptLevel,
    List<TagDto>     tags,
    List<ExampleDto> examples
) {
    public record ExampleDto(
        String japanese,
        String english,
        String tatoebaId,
        String form
    ) {}
}