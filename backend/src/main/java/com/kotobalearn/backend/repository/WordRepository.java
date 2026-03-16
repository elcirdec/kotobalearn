package com.kotobalearn.backend.repository;

import com.kotobalearn.backend.dto.WordSummaryDto;
import com.kotobalearn.backend.model.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WordRepository extends JpaRepository<Word, Integer> {

    // ── JLPT ──────────────────────────────────────────────────────────────
    @Query("SELECT w FROM Word w WHERE w.jlptLevel.jlptCode = :jlpt")
    Page<Word> findByJlpt(@Param("jlpt") String jlpt, Pageable pageable);

    // ── Recherche rankée + tri par longueur ───────────────────────────────
    // Rang :  1 = exact JP
    //         2 = commence par JP
    //         3 = exact EN
    //         4 = commence par EN
    //         5 = contient JP
    //         6 = contient EN
    // Tri secondaire : LEAST(length) → le plus court remonte en premier
    // à rang égal, puis word_id pour la stabilité
    @Query(
        value = """
            SELECT * FROM word
            WHERE word_japanese               ILIKE '%' || :q || '%'
               OR word_pronunciation_hiragana ILIKE '%' || :q || '%'
               OR word_translation_en         ILIKE '%' || :q || '%'
            ORDER BY
              CASE
                WHEN word_japanese = :q                              THEN 1
                WHEN word_japanese ILIKE :q || '%'                  THEN 2
                WHEN LOWER(word_translation_en) = :qs               THEN 3
                WHEN word_translation_en ILIKE :q || '%'            THEN 4
                WHEN word_japanese ILIKE '%' || :q || '%'           THEN 5
                WHEN word_translation_en ILIKE '%' || :q || '%'     THEN 6
                ELSE 7
              END,
              LEAST(
                LENGTH(word_japanese),
                LENGTH(COALESCE(word_translation_en, word_japanese))
              ),
              word_id
            """,
        countQuery = """
            SELECT COUNT(*) FROM word
            WHERE word_japanese               ILIKE '%' || :q || '%'
               OR word_pronunciation_hiragana ILIKE '%' || :q || '%'
               OR word_translation_en         ILIKE '%' || :q || '%'
            """,
        nativeQuery = true
    )
    Page<Word> searchRanked(@Param("q") String q, @Param("qs") String qs, Pageable pageable);

    @Query(
        value = """
            SELECT w.* FROM word w
            JOIN jlpt_level j ON w.word_jlpt_id = j.jlpt_id
            WHERE j.jlpt_code = :jlpt
              AND (w.word_japanese               ILIKE '%' || :q || '%'
                OR w.word_pronunciation_hiragana ILIKE '%' || :q || '%'
                OR w.word_translation_en         ILIKE '%' || :q || '%')
            ORDER BY
              CASE
                WHEN w.word_japanese = :q                              THEN 1
                WHEN w.word_japanese ILIKE :q || '%'                  THEN 2
                WHEN LOWER(w.word_translation_en) = :qs               THEN 3
                WHEN w.word_translation_en ILIKE :q || '%'            THEN 4
                WHEN w.word_japanese ILIKE '%' || :q || '%'           THEN 5
                WHEN w.word_translation_en ILIKE '%' || :q || '%'     THEN 6
                ELSE 7
              END,
              LEAST(
                LENGTH(w.word_japanese),
                LENGTH(COALESCE(w.word_translation_en, w.word_japanese))
              ),
              w.word_id
            """,
        countQuery = """
            SELECT COUNT(*) FROM word w
            JOIN jlpt_level j ON w.word_jlpt_id = j.jlpt_id
            WHERE j.jlpt_code = :jlpt
              AND (w.word_japanese               ILIKE '%' || :q || '%'
                OR w.word_pronunciation_hiragana ILIKE '%' || :q || '%'
                OR w.word_translation_en         ILIKE '%' || :q || '%')
            """,
        nativeQuery = true
    )
    Page<Word> searchRankedByJlpt(@Param("jlpt") String jlpt, @Param("q") String q, @Param("qs") String qs, Pageable pageable);

    // ── Multi-tags OR ──────────────────────────────────────────────────────
    @Query("SELECT DISTINCT w FROM Word w JOIN w.tags t WHERE t.tagCode IN :tagCodes")
    Page<Word> findByTagsOr(@Param("tagCodes") List<String> tagCodes, Pageable pageable);

    @Query("""
        SELECT DISTINCT w FROM Word w JOIN w.tags t
        WHERE t.tagCode IN :tagCodes AND w.jlptLevel.jlptCode = :jlpt
        """)
    Page<Word> findByTagsOrAndJlpt(@Param("tagCodes") List<String> tagCodes, @Param("jlpt") String jlpt, Pageable pageable);

    // ── Multi-tags AND ─────────────────────────────────────────────────────
    @Query("""
        SELECT w FROM Word w JOIN w.tags t
        WHERE t.tagCode IN :tagCodes
        GROUP BY w HAVING COUNT(DISTINCT t.tagCode) = :tagCount
        """)
    Page<Word> findByTagsAnd(@Param("tagCodes") List<String> tagCodes, @Param("tagCount") long tagCount, Pageable pageable);

    @Query("""
        SELECT w FROM Word w JOIN w.tags t
        WHERE t.tagCode IN :tagCodes AND w.jlptLevel.jlptCode = :jlpt
        GROUP BY w HAVING COUNT(DISTINCT t.tagCode) = :tagCount
        """)
    Page<Word> findByTagsAndAndJlpt(@Param("tagCodes") List<String> tagCodes, @Param("jlpt") String jlpt, @Param("tagCount") long tagCount, Pageable pageable);

    // ── Mots d'un kanji ────────────────────────────────────────────────────
    @Query("""
        SELECT new com.kotobalearn.backend.dto.WordSummaryDto(
            w.wordId, w.wordJapanese, w.wordPronunciationHiragana, w.wordTranslationEn
        )
        FROM Word w JOIN w.kanjis k WHERE k.kanjiId = :kanjiId
        """)
    Page<WordSummaryDto> findByKanjiId(@Param("kanjiId") Integer kanjiId, Pageable pageable);
}