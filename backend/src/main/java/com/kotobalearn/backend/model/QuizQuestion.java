package com.kotobalearn.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_question")
@Getter @Setter
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qq_id")
    private Integer qqId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qs_id", nullable = false)
    private QuizSession quizSession;

    @Column(name = "qq_element_type", nullable = false, length = 20)
    private String qqElementType;

    @Column(name = "qq_element_id", nullable = false)
    private Integer qqElementId;

    @Column(name = "qq_question_type", nullable = false, length = 20)
    private String qqQuestionType;

    @Column(name = "qq_user_answer", columnDefinition = "TEXT")
    private String qqUserAnswer;

    @Column(name = "qq_is_correct")
    private Boolean qqIsCorrect;

    @Column(name = "qq_time_taken")
    private Integer qqTimeTaken;
}