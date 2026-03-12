package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "example")
@Getter @Setter
public class Example {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ex_id")
    private Integer exId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kanji_id", nullable = false)
    private Kanji kanji;

    @Column(name = "ex_japanese", nullable = false)
    private String exJapanese;

    @Column(name = "ex_meaning_english", columnDefinition = "TEXT")
    private String exMeaningEnglish;

    @Column(name = "ex_audio_opus_url")
    private String exAudioOpusUrl;

    @Column(name = "ex_audio_aac_url")
    private String exAudioAacUrl;

    @Column(name = "ex_audio_ogg_url")
    private String exAudioOggUrl;

    @Column(name = "ex_audio_mp3_url")
    private String exAudioMp3Url;
}