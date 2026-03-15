package com.kotobalearn.backend.repository;

import com.kotobalearn.backend.dto.WordSummaryDto;
import com.kotobalearn.backend.model.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WordRepository extends JpaRepository<Word, Integer> {

    // Filtre par niveau JLPT
    @Query("SELECT w FROM Word w WHERE w.jlptLevel.jlptCode = :jlpt")
    Page<Word> findByJlpt(@Param("jlpt") String jlpt, Pageable pageable);

    // Filtre par tag_code
    @Query("""
        SELECT DISTINCT w FROM Word w
        JOIN w.tags t
        WHERE t.tagCode = :tagCode
        """)
    Page<Word> findByTagCode(@Param("tagCode") String tagCode, Pageable pageable);

    // Filtre par tag_type
    @Query("""
        SELECT DISTINCT w FROM Word w
        JOIN w.tags t
        WHERE t.tagType = :tagType
        """)
    Page<Word> findByTagType(@Param("tagType") String tagType, Pageable pageable);

    // Recherche japonais OU traduction anglaise
    @Query("""
        SELECT w FROM Word w
        WHERE w.wordJapanese LIKE :q
           OR w.wordPronunciationHiragana LIKE :q
           OR w.wordTranslationEn LIKE :q
        """)
    Page<Word> search(@Param("q") String q, Pageable pageable);

    // Combiné : recherche + JLPT
    @Query("""
        SELECT w FROM Word w
        WHERE w.jlptLevel.jlptCode = :jlpt
          AND (w.wordJapanese LIKE :q OR w.wordTranslationEn LIKE :q)
        """)
    Page<Word> searchByJlpt(@Param("jlpt") String jlpt, @Param("q") String q, Pageable pageable);

    // ── Mots contenant ce kanji (via word_kanji) ────────────────
 
    @Query("""
        SELECT new com.kotobalearn.backend.dto.WordSummaryDto(
            w.wordId,
            w.wordJapanese,
            w.wordPronunciationHiragana,
            w.wordTranslationEn
        )
        FROM Word w JOIN w.kanjis k
        WHERE k.kanjiId = :kanjiId
        """)
    Page<WordSummaryDto> findByKanjiId(@Param("kanjiId") Integer kanjiId, Pageable pageable);
}