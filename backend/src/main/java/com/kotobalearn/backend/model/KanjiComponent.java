package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "kanji_component",
       uniqueConstraints = @UniqueConstraint(columnNames = {"kanji_id", "radical_id"}))
@Getter @Setter
public class KanjiComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kc_id")
    private Integer kcId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kanji_id", nullable = false)
    private Kanji kanji;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "radical_id", nullable = false)
    private Radical radical;

    /** Position du composant dans la ligne KRADFILE (0 = premier = radical principal) */
    @Column(name = "kc_position")
    private Short kcPosition;
}