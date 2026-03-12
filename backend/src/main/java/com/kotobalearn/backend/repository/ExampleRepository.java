package com.kotobalearn.backend.repository;

import com.kotobalearn.backend.model.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExampleRepository extends JpaRepository<Example, Integer> {

    List<Example> findByKanji_KanjiId(Integer kanjiId);
}