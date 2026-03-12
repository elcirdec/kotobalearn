package com.kotobalearn.backend.repository;

import com.kotobalearn.backend.model.Script;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ScriptRepository extends JpaRepository<Script, Integer> {

    List<Script> findByScType(String scType); // 'HIRAGANA' ou 'KATAKANA'

    Optional<Script> findByScCharacter(String character);
}