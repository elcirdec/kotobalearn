package com.kotobalearn.backend.repository;

import com.kotobalearn.backend.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Integer> {
}