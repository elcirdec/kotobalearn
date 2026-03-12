package com.kotobalearn.backend.service;

import com.kotobalearn.backend.dto.ScriptDto;
import com.kotobalearn.backend.repository.ScriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScriptService {

    private final ScriptRepository scriptRepository;

    public List<ScriptDto> findAll() {
        return scriptRepository.findAll()
                .stream().map(s -> {
                    ScriptDto dto = new ScriptDto();
                    dto.setScId(s.getScId());
                    dto.setScType(s.getScType());
                    dto.setScCharacter(s.getScCharacter());
                    dto.setScRomaji(s.getScRomaji());
                    return dto;
                }).toList();
    }

    public List<ScriptDto> findByType(String type) {
        return scriptRepository.findByScType(type.toUpperCase())
                .stream().map(s -> {
                    ScriptDto dto = new ScriptDto();
                    dto.setScId(s.getScId());
                    dto.setScType(s.getScType());
                    dto.setScCharacter(s.getScCharacter());
                    dto.setScRomaji(s.getScRomaji());
                    return dto;
                }).toList();
    }
}