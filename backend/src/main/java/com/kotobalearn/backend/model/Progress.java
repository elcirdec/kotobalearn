package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "progress")
@Getter @Setter
public class Progress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prog_id")
    private Integer progId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usr_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "prog_element_type", nullable = false, length = 20)
    private String progElementType;

    @Column(name = "prog_element_id", nullable = false)
    private Integer progElementId;

    @Column(name = "prog_status", nullable = false, length = 20)
    private String progStatus;

    @Column(name = "prog_last_reviewed")
    private LocalDateTime progLastReviewed;

    @Column(name = "prog_next_review")
    private LocalDateTime progNextReview;

    @Column(name = "prog_correct_count", nullable = false)
    private Integer progCorrectCount = 0;

    @Column(name = "prog_wrong_count", nullable = false)
    private Integer progWrongCount = 0;
}