package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "translation")
@Getter @Setter
public class Translation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trans_id")
    private Integer transId;

    @Column(name = "trans_element_type", nullable = false, length = 20)
    private String transElementType;

    @Column(name = "trans_element_id", nullable = false)
    private Integer transElementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lang_id", nullable = false)
    private Language language;

    @Column(name = "trans_text", nullable = false, columnDefinition = "TEXT")
    private String transText;
}