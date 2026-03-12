package com.kotobalearn.backend.controller;

import com.kotobalearn.backend.dto.ScriptDto;
import com.kotobalearn.backend.service.ScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/scripts")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptService scriptService;

    // GET /api/scripts
    // GET /api/scripts?type=HIRAGANA
    // GET /api/scripts?type=KATAKANA
    @GetMapping
    public List<ScriptDto> getAll(
            @RequestParam(required = false) String type) {

        if (type != null) return scriptService.findByType(type);
        return scriptService.findAll();
    }
}