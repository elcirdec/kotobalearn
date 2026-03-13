package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "word_example")
@Getter @Setter
public class WordExample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "we_id")
    private Integer weId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;

    @Column(name = "we_japanese", nullable = false, columnDefinition = "TEXT")
    private String weJapanese;

    @Column(name = "we_english", nullable = false, columnDefinition = "TEXT")
    private String weEnglish;

    @Column(name = "we_tatoeba_id", length = 20)
    private String weTatoebaId;

    @Column(name = "we_form", length = 100)
    private String weForm;
}