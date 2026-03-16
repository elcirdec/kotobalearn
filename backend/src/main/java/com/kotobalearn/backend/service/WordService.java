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

    /**
     * Recherche principale.
     * La recherche textuelle est automatiquement rankée :
     *   exact > commence par > contient (japonais puis anglais)
     */
    public Page<WordSummaryDto> findAll(
        String jlpt, List<String> tags, String tagMode,
        String search, int page, int size
    ) {
        // On ignore le tri par wordId pour la recherche (le rank prime)
        Pageable paginatedOnly  = PageRequest.of(page, size);
        Pageable sortedByWordId = PageRequest.of(page, size, Sort.by("wordId"));

        boolean hasJlpt   = jlpt   != null && !jlpt.isBlank();
        boolean hasTags   = tags   != null && !tags.isEmpty();
        boolean hasSearch = search != null && !search.isBlank();
        boolean isAnd     = "and".equalsIgnoreCase(tagMode);

        Page<Word> words;

        if (hasTags && hasJlpt) {
            words = isAnd
                ? wordRepository.findByTagsAndAndJlpt(tags, jlpt, (long) tags.size(), sortedByWordId)
                : wordRepository.findByTagsOrAndJlpt(tags, jlpt, sortedByWordId);
        } else if (hasTags) {
            words = isAnd
                ? wordRepository.findByTagsAnd(tags, (long) tags.size(), sortedByWordId)
                : wordRepository.findByTagsOr(tags, sortedByWordId);
        } else if (hasSearch && hasJlpt) {
            words = wordRepository.searchRankedByJlpt(jlpt, search, search.toLowerCase(), paginatedOnly);
        } else if (hasSearch) {
            words = wordRepository.searchRanked(search, search.toLowerCase(), paginatedOnly);
        } else if (hasJlpt) {
            words = wordRepository.findByJlpt(jlpt, sortedByWordId);
        } else {
            words = wordRepository.findAll(sortedByWordId);
        }

        return words.map(this::toSummary);
    }

    public WordDetailDto findById(Integer id) {
        Word word = wordRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Mot introuvable : " + id));
        return toDetail(word);
    }

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
                .map(e -> new ExampleDto(e.getWeJapanese(), e.getWeEnglish(), e.getWeTatoebaId(), e.getWeForm()))
                .toList();
        return new WordDetailDto(
            w.getWordId(), w.getWordJapanese(), w.getWordPronunciationHiragana(),
            w.getWordTranslationEn(), w.getWordTranslationFr(),
            w.getJlptLevel() != null ? w.getJlptLevel().getJlptCode() : null,
            tags, examples
        );
    }
}