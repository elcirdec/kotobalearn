package com.kotobalearn.backend.repository;

import com.kotobalearn.backend.model.Reading;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReadingRepository extends JpaRepository<Reading, Integer> {

    List<Reading> findByKanji_KanjiId(Integer kanjiId);

    List<Reading> findByReadType(String readType); // 'ON' ou 'KUN'
}