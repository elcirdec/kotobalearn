package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "language")
@Getter @Setter
public class Language {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lang_id")
    private Integer langId;

    @Column(name = "lang_code", nullable = false, unique = true, length = 10)
    private String langCode;

    @Column(name = "lang_name", nullable = false, length = 50)
    private String langName;
}