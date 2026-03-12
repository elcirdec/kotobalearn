package com.kotobalearn.backend.repository;

import com.kotobalearn.backend.model.Kanji;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface KanjiRepository extends JpaRepository<Kanji, Integer> {

    Optional<Kanji> findByKanjiCharacter(String character);

    List<Kanji> findByKanjiGrade(Integer grade);

    List<Kanji> findByJlptLevel_JlptCode(String jlptCode);

    // Recherche par sens (ex: "love" trouve 愛)
    List<Kanji> findByKanjiMeaningEnglishContainingIgnoreCase(String meaning);
}