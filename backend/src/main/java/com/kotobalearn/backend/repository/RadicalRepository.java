package com.kotobalearn.backend.repository;

import com.kotobalearn.backend.model.Radical;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RadicalRepository extends JpaRepository<Radical, Integer> {

    Optional<Radical> findByRadCharacter(String character);
}