package com.kotobalearn.backend.dto;

import lombok.Data;

@Data
public class ExampleDto {
    private Integer exId;
    private String exJapanese;
    private String exMeaningEnglish;
    private String exAudioMp3Url;   // On expose seulement mp3 pour simplifier
}