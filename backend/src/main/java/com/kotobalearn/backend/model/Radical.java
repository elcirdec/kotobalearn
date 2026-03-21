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

    @Column(name = "rad_character")
    private String radCharacter;

    @Column(name = "rad_strokes")
    private Integer radStrokes;

    @Column(name = "rad_name_hiragana")
    private String radNameHiragana;

    @Column(name = "rad_name_romaji")
    private String radNameRomaji;

    @Column(name = "rad_meaning_english")
    private String radMeaningEnglish;

    @Column(name = "rad_position_romaji")
    private String radPositionRomaji;

    // ── Champs KanjiAlive ──────────────────────────────────────────────────

    /** URL du SVG du radical (KanjiAlive CDN) */
    @Column(name = "rad_image_url")
    private String radImageUrl;

    /** Position en hiragana (ex: へん, かんむり…) */
    @Column(name = "rad_position_hiragana")
    private String radPositionHiragana;

    /** URL de l'icône de position (SVG KanjiAlive) */
    @Column(name = "rad_position_icon_url")
    private String radPositionIconUrl;

    /**
     * Type de l'entrée :
     * 'radical'   = radical traditionnel Kangxi (KanjiAlive)
     * 'component' = composant visuel KRADFILE (EDRDG)
     */
    @Column(name = "rad_type")
    private String radType;

    /** Frames d'animation SVG (KanjiAlive) */
    @OneToMany(mappedBy = "radical", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RadicalAnimation> animations;
}