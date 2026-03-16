package com.kotobalearn.backend.repository;

import com.kotobalearn.backend.model.Kanji;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KanjiRepository extends JpaRepository<Kanji, Integer> {

    Optional<Kanji> findByKanjiCharacter(String character);
    List<Kanji> findByKanjiGrade(Integer grade);
    List<Kanji> findByJlptLevel_JlptCode(String jlptCode);
    List<Kanji> findByKanjiStrokes(Integer strokes);

    // ── Recherche texte ────────────────────────────────────────────────────
    List<Kanji> findByKanjiMeaningEnglishContainingIgnoreCase(String meaning);

    // ── Multi-radicaux (IN) ────────────────────────────────────────────────
    @Query("SELECT k FROM Kanji k WHERE k.radical.radId IN :radIds")
    List<Kanji> findByRadicalIds(@Param("radIds") List<Integer> radIds);

    // ── Combinaisons JLPT + Grade ──────────────────────────────────────────
    @Query("SELECT k FROM Kanji k WHERE k.jlptLevel.jlptCode = :jlpt AND k.kanjiGrade = :grade")
    List<Kanji> findByJlptAndGrade(@Param("jlpt") String jlpt, @Param("grade") Integer grade);

    // ── Combinaisons Strokes ───────────────────────────────────────────────
    @Query("SELECT k FROM Kanji k WHERE k.kanjiStrokes = :s AND k.jlptLevel.jlptCode = :jlpt")
    List<Kanji> findByStrokesAndJlpt(@Param("s") Integer s, @Param("jlpt") String jlpt);

    @Query("SELECT k FROM Kanji k WHERE k.kanjiStrokes = :s AND k.kanjiGrade = :grade")
    List<Kanji> findByStrokesAndGrade(@Param("s") Integer s, @Param("grade") Integer grade);

    @Query("SELECT k FROM Kanji k WHERE k.kanjiStrokes = :s AND k.jlptLevel.jlptCode = :jlpt AND k.kanjiGrade = :grade")
    List<Kanji> findByStrokesJlptGrade(@Param("s") Integer s, @Param("jlpt") String jlpt, @Param("grade") Integer grade);
}