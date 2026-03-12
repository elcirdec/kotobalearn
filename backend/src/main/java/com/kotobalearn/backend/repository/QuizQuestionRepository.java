package com.kotobalearn.backend.repository;

import com.kotobalearn.backend.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Integer> {

    List<QuizQuestion> findByQuizSession_QsId(Integer sessionId);
}