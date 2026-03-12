package com.kotobalearn.backend.repository;

import com.kotobalearn.backend.model.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizSessionRepository extends JpaRepository<QuizSession, Integer> {

    List<QuizSession> findByUtilisateur_UsrIdOrderByQsStartTimeDesc(Integer userId);
}