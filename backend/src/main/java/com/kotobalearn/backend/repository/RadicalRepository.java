package com.kotobalearn.backend.repository;

import com.kotobalearn.backend.model.Radical;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface RadicalRepository extends JpaRepository<Radical, Integer> {

    Optional<Radical> findByRadCharacter(String character);

    @Query("SELECT DISTINCT r FROM Radical r JOIN KanjiComponent kc ON kc.radical = r")
    List<Radical> findActiveComponents();
}