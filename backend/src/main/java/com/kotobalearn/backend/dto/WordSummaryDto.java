package com.kotobalearn.backend.dto;

public record WordSummaryDto(
    Integer wordId,
    String  wordJapanese,
    String  wordReading,
    String  wordTranslationEn
) {}