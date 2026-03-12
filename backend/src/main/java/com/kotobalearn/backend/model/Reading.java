package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reading")
@Getter @Setter
public class Reading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "read_id")
    private Integer readId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kanji_id", nullable = false)
    private Kanji kanji;

    // 'ON' ou 'KUN' — validé par contrainte CHECK en base
    @Column(name = "read_type", nullable = false, length = 10)
    private String readType;

    @Column(name = "read_kana", nullable = false, length = 50)
    private String readKana;

    @Column(name = "read_romaji", nullable = false, length = 50)
    private String readRomaji;
}