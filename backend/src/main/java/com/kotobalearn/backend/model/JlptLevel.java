package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "jlpt_level")
@Getter @Setter
public class JlptLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "jlpt_id")
    private Integer jlptId;

    @Column(name = "jlpt_code", nullable = false, unique = true, length = 5)
    private String jlptCode;

    @Column(name = "jlpt_description", length = 100)
    private String jlptDescription;
}