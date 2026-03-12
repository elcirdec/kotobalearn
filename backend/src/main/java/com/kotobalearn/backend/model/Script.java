package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "script")
@Getter @Setter
public class Script {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sc_id")
    private Integer scId;

    // 'HIRAGANA' ou 'KATAKANA'
    @Column(name = "sc_type", nullable = false, length = 10)
    private String scType;

    @Column(name = "sc_character", nullable = false, unique = true, length = 10)
    private String scCharacter;

    @Column(name = "sc_romaji", nullable = false, length = 20)
    private String scRomaji;
}