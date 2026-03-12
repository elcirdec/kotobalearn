package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity
@Table(name = "kanji")
@Getter @Setter
public class Kanji {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kanji_id")
    private Integer kanjiId;

    @Column(name = "kanji_character", nullable = false, unique = true, length = 10)
    private String kanjiCharacter;

    @Column(name = "kanji_meaning_english", columnDefinition = "TEXT")
    private String kanjiMeaningEnglish;

    @Column(name = "kanji_strokes", nullable = false)
    private Integer kanjiStrokes;

    @Column(name = "kanji_video_poster_url")
    private String kanjiVideoPosterUrl;

    @Column(name = "kanji_video_mp4_url")
    private String kanjiVideoMp4Url;

    @Column(name = "kanji_video_webm_url")
    private String kanjiVideoWebmUrl;

    @Column(name = "kanji_grade")
    private Integer kanjiGrade;

    @Column(name = "kanji_kodansha", length = 20)
    private String kanjiKodansha;

    @Column(name = "kanji_classic_nelson", length = 20)
    private String kanjiClassicNelson;

    // Radical associé (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rad_id")
    private Radical radical;

    // Niveau JLPT (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jlpt_id")
    private JlptLevel jlptLevel;

    // Lectures (on'yomi et kun'yomi)
    @OneToMany(mappedBy = "kanji", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reading> readings;

    // Exemples de mots avec audio
    @OneToMany(mappedBy = "kanji", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Example> examples;
}