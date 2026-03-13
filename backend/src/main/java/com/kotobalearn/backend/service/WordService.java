package com.kotobalearn.backend.service;

import com.kotobalearn.backend.dto.TagDto;
import com.kotobalearn.backend.dto.WordDetailDto;
import com.kotobalearn.backend.dto.WordDetailDto.ExampleDto;
import com.kotobalearn.backend.dto.WordSummaryDto;
import com.kotobalearn.backend.model.Word;
import com.kotobalearn.backend.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WordService {

    private final WordRepository wordRepository;

    public Page<WordSummaryDto> findAll(String jlpt, String tag, String tagType,
                                        String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("wordId"));

        Page<Word> words;

        if (search != null && !search.isBlank() && jlpt != null && !jlpt.isBlank()) {
            words = wordRepository.searchByJlpt(jlpt, "%" + search + "%", pageable);
        } else if (search != null && !search.isBlank()) {
            words = wordRepository.search("%" + search + "%", pageable);
        } else if (jlpt != null && !jlpt.isBlank()) {
            words = wordRepository.findByJlpt(jlpt, pageable);
        } else if (tag != null && !tag.isBlank()) {
            words = wordRepository.findByTagCode(tag, pageable);
        } else if (tagType != null && !tagType.isBlank()) {
            words = wordRepository.findByTagType(tagType, pageable);
        } else {
            words = wordRepository.findAll(pageable);
        }

        return words.map(this::toSummary);
    }

    public WordDetailDto findById(Integer id) {
        Word word = wordRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Mot introuvable : " + id));
        return toDetail(word);
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private WordSummaryDto toSummary(Word w) {
        return new WordSummaryDto(
            w.getWordId(),
            w.getWordJapanese(),
            w.getWordPronunciationHiragana(),
            w.getWordTranslationEn()
        );
    }

    private WordDetailDto toDetail(Word w) {
        List<TagDto> tags = w.getTags() == null ? List.of() :
            w.getTags().stream().map(TagDto::from).toList();

        List<ExampleDto> examples = w.getExamples() == null ? List.of() :
            w.getExamples().stream()
                .map(e -> new ExampleDto(
                    e.getWeJapanese(),
                    e.getWeEnglish(),
                    e.getWeTatoebaId(),
                    e.getWeForm()
                ))
                .toList();

        return new WordDetailDto(
            w.getWordId(),
            w.getWordJapanese(),
            w.getWordPronunciationHiragana(),
            w.getWordTranslationEn(),
            w.getWordTranslationFr(),
            w.getJlptLevel() != null ? w.getJlptLevel().getJlptCode() : null,
            tags,
            examples
        );
    }
}