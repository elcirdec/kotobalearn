package com.kotobalearn.backend.dto;

import lombok.Data;

@Data
public class ScriptDto {
    private Integer scId;
    private String scType;
    private String scCharacter;
    private String scRomaji;
}