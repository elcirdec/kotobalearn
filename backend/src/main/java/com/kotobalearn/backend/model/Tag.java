package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tag")
@Getter @Setter
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Integer tagId;

    @Column(name = "tag_code", nullable = false, length = 50)
    private String tagCode;

    @Column(name = "tag_type", nullable = false, length = 20)
    private String tagType;

    @Column(name = "tag_label", nullable = false, columnDefinition = "TEXT")
    private String tagLabel;
}