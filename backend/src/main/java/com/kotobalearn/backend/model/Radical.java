package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity
@Table(name = "radical")
@Getter @Setter
public class Radical {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rad_id")
    private Integer radId;

    @Column(name = "rad_character", nullable = false, length = 10)
    private String radCharacter;

    @Column(name = "rad_strokes", nullable = false)
    private Integer radStrokes;

    @Column(name = "rad_image_url")
    private String radImageUrl;

    @Column(name = "rad_name_hiragana", length = 50)
    private String radNameHiragana;

    @Column(name = "rad_name_romaji", length = 50)
    private String radNameRomaji;

    @Column(name = "rad_meaning_english", columnDefinition = "TEXT")
    private String radMeaningEnglish;

    @Column(name = "rad_position_hiragana", length = 20)
    private String radPositionHiragana;

    @Column(name = "rad_position_romaji", length = 20)
    private String radPositionRomaji;

    @Column(name = "rad_position_icon_url")
    private String radPositionIconUrl;

    // Un radical a plusieurs frames d'animation
    @OneToMany(mappedBy = "radical", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("raOrder ASC")
    private List<RadicalAnimation> animations;
}