package com.kotobalearn.backend.repository;

import com.kotobalearn.backend.model.KanjiComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KanjiComponentRepository extends JpaRepository<KanjiComponent, Integer> {

    /**
     * Retourne tous les composants d'un kanji, triés par position.
     */
    @Query("""
        SELECT kc FROM KanjiComponent kc
        JOIN FETCH kc.radical
        WHERE kc.kanji.kanjiId = :kanjiId
        ORDER BY kc.kcPosition
        """)
    List<KanjiComponent> findByKanjiId(@Param("kanjiId") Integer kanjiId);
}