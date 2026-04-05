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
    List<Kanji> findByKanjiGradeIn(List<Integer> grades);
    List<Kanji> findByJlptLevel_JlptCode(String jlptCode);
    List<Kanji> findByKanjiStrokes(Integer strokes);
    List<Kanji> findByKanjiMeaningEnglishContainingIgnoreCase(String meaning);

    @Query("SELECT k FROM Kanji k WHERE k.radical.radId IN :radIds")
    List<Kanji> findByRadicalIds(@Param("radIds") List<Integer> radIds);

    @Query("""
        SELECT k FROM Kanji k
        WHERE (
            SELECT COUNT(DISTINCT kc.radical.radId)
            FROM KanjiComponent kc
            WHERE kc.kanji = k
              AND kc.radical.radId IN :radIds
        ) = :radCount
        """)
    List<Kanji> findByAllComponents(
        @Param("radIds")   List<Integer> radIds,
        @Param("radCount") long radCount
    );

    @Query("SELECT k FROM Kanji k WHERE k.jlptLevel.jlptCode = :jlpt AND k.kanjiGrade = :grade")
    List<Kanji> findByJlptAndGrade(@Param("jlpt") String jlpt, @Param("grade") Integer grade);

    @Query("SELECT k FROM Kanji k WHERE k.jlptLevel.jlptCode = :jlpt AND k.kanjiGrade IN :grades")
    List<Kanji> findByJlptAndGradeIn(@Param("jlpt") String jlpt, @Param("grades") List<Integer> grades);

    @Query("SELECT k FROM Kanji k WHERE k.kanjiStrokes = :s AND k.jlptLevel.jlptCode = :jlpt")
    List<Kanji> findByStrokesAndJlpt(@Param("s") Integer s, @Param("jlpt") String jlpt);

    @Query("SELECT k FROM Kanji k WHERE k.kanjiStrokes = :s AND k.kanjiGrade = :grade")
    List<Kanji> findByStrokesAndGrade(@Param("s") Integer s, @Param("grade") Integer grade);

    @Query("SELECT k FROM Kanji k WHERE k.kanjiStrokes = :s AND k.jlptLevel.jlptCode = :jlpt AND k.kanjiGrade = :grade")
    List<Kanji> findByStrokesJlptGrade(@Param("s") Integer s, @Param("jlpt") String jlpt, @Param("grade") Integer grade);

    @Query("SELECT k FROM Kanji k WHERE k.kanjiStrokes = :s AND k.kanjiGrade IN :grades")
    List<Kanji> findByStrokesAndGradeIn(@Param("s") Integer s, @Param("grades") List<Integer> grades);

    // Seule requête stroke-counts conservée — celle d'origine qui fonctionne
    @Query("SELECT DISTINCT k.kanjiStrokes FROM Kanji k WHERE k.kanjiStrokes IS NOT NULL ORDER BY k.kanjiStrokes")
    List<Integer> findDistinctStrokeCounts();
}