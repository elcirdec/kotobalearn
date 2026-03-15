package com.kotobalearn.backend.controller;

import com.kotobalearn.backend.dto.TagDto;
import com.kotobalearn.backend.model.Tag;
import com.kotobalearn.backend.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagRepository tagRepository;

    /**
     * GET /api/tags
     * Retourne tous les tags (201 entrées) pour alimenter les filtres frontend.
     * On exclut ke_inf (peu utile pour l'utilisateur).
     */
    @GetMapping
    public ResponseEntity<List<TagDto>> getAllTags() {
        List<Tag> tags = tagRepository.findAll();
        List<TagDto> dtos = tags.stream()
            .filter(t -> !t.getTagType().equals("ke_inf"))
            .sorted((a, b) -> {
                int typeCompare = a.getTagType().compareTo(b.getTagType());
                if (typeCompare != 0) return typeCompare;
                return a.getTagLabel().compareTo(b.getTagLabel());
            })
            .map(TagDto::from)
            .toList();
        return ResponseEntity.ok(dtos);
    }
}