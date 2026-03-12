package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "quiz_session")
@Getter @Setter
public class QuizSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qs_id")
    private Integer qsId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usr_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "qs_start_time", nullable = false)
    private LocalDateTime qsStartTime;

    @Column(name = "qs_end_time")
    private LocalDateTime qsEndTime;

    @Column(name = "qs_type", length = 100)
    private String qsType;

    @OneToMany(mappedBy = "quizSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizQuestion> questions;
}