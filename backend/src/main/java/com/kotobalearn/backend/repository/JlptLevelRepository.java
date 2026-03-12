package com.kotobalearn.backend.repository;

import com.kotobalearn.backend.model.JlptLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JlptLevelRepository extends JpaRepository<JlptLevel, Integer> {

    Optional<JlptLevel> findByJlptCode(String code); // ex: "N5"
}