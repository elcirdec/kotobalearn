package com.kotobalearn.backend.dto;

import lombok.Data;

@Data
public class ReadingDto {
    private Integer readId;
    private String readType;   // 'ON' ou 'KUN'
    private String readKana;
    private String readRomaji;
}