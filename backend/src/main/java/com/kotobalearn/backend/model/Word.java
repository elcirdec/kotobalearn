package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "word")
@Getter @Setter
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "word_id")
    private Integer wordId;

    @Column(name = "word_japanese", nullable = false)
    private String wordJapanese;

    @Column(name = "word_pronunciation_hiragana", nullable = false)
    private String wordPronunciationHiragana;

    @Column(name = "word_romaji", nullable = false)
    private String wordRomaji;

    @Column(name = "word_translation_en", columnDefinition = "TEXT")
    private String wordTranslationEn;

    @Column(name = "word_translation_fr", columnDefinition = "TEXT")
    private String wordTranslationFr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_jlpt_id")
    private JlptLevel jlptLevel;

    // Catégories du mot (many-to-many via word_category)
    @ManyToMany
    @JoinTable(
        name = "word_category",
        joinColumns = @JoinColumn(name = "word_id"),
        inverseJoinColumns = @JoinColumn(name = "cat_id")
    )
    private List<Category> categories;

    // Kanji composant le mot (many-to-many via word_kanji)
    @ManyToMany
    @JoinTable(
        name = "word_kanji",
        joinColumns = @JoinColumn(name = "word_id"),
        inverseJoinColumns = @JoinColumn(name = "kanji_id")
    )
    private List<Kanji> kanjis;

    @ManyToMany
    @JoinTable(
        name = "word_tag",
        joinColumns = @JoinColumn(name = "word_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;

    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WordExample> examples = new ArrayList<>();
}