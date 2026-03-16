package com.kotobalearn.backend.controller;

import com.kotobalearn.backend.dto.RadicalDto;
import com.kotobalearn.backend.repository.RadicalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/radicals")
@RequiredArgsConstructor
public class RadicalController {

    private final RadicalRepository radicalRepository;

    /**
     * GET /api/radicals
     * Retourne tous les radicaux triés par nombre de traits puis romaji.
     * Utilise un DTO pour éviter le lazy loading des animations.
     */
    @GetMapping
    public ResponseEntity<List<RadicalDto>> getAll() {
        List<RadicalDto> radicals = radicalRepository.findAll()
            .stream()
            .map(RadicalDto::from)
            .sorted(Comparator
                .comparingInt((RadicalDto r) -> r.radStrokes() != null ? r.radStrokes() : 0)
                .thenComparing(r -> r.radNameRomaji() != null ? r.radNameRomaji() : "")
            )
            .toList();
        return ResponseEntity.ok(radicals);
    }
}