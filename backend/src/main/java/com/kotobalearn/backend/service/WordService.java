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
     * @param sortBy  "relevance" (défaut) ou "frequency"
     *                relevance  → pertinence textuelle (exact > commence > contient)
     *                frequency  → mots les plus courants en premier (word_frequency_rank)
     */
    public Page<WordSummaryDto> findAll(
        String jlpt, List<String> tags, String tagMode,
        String search, String sortBy,
        int page, int size
    ) {
        // Valeurs sécurisées (jamais null)
    List<String> safeTags = tags == null ? List.of() : tags;
    String safeSearch = search == null ? "" : search;
    String searchLower = safeSearch.toLowerCase();

    boolean hasJlpt   = jlpt   != null && !jlpt.isBlank();
    boolean hasTags   = !safeTags.isEmpty();
    boolean hasSearch = !safeSearch.isBlank();
    boolean isAnd     = "and".equalsIgnoreCase(tagMode);
    boolean byFreq    = "frequency".equalsIgnoreCase(sortBy);

        // Pageables
        Pageable byWordId   = PageRequest.of(page, size, Sort.by("wordId"));
        Pageable byFreqSort = PageRequest.of(page, size,
            Sort.by(Sort.Order.asc("wordFrequencyRank").nullsLast(),
                    Sort.Order.asc("wordId")));
        Pageable noSort     = PageRequest.of(page, size); // pour les native queries (tri dans SQL)

        Page<Word> words;

        if (hasTags && hasJlpt) {
            Pageable p = byFreq ? byFreqSort : byWordId;
            words = isAnd
                ? wordRepository.findByTagsAndAndJlpt(safeTags, jlpt, (long) safeTags.size(), p)
                : wordRepository.findByTagsOrAndJlpt(safeTags, jlpt, p);

        } else if (hasTags) {
            Pageable p = byFreq ? byFreqSort : byWordId;
            words = isAnd
                ? wordRepository.findByTagsAnd(safeTags, (long) safeTags.size(), p)
                : wordRepository.findByTagsOr(safeTags, p);

        } else if (hasSearch && hasJlpt) {
            words = byFreq
                ? wordRepository.searchByFrequencyAndJlpt(jlpt, safeSearch, searchLower, noSort)
                : wordRepository.searchRankedByJlpt(jlpt, safeSearch, searchLower, noSort);

        } else if (hasSearch) {
            words = byFreq
                ? wordRepository.searchByFrequency(safeSearch, searchLower, noSort)
                : wordRepository.searchRanked(safeSearch, searchLower, noSort);

        } else if (hasJlpt) {
            Pageable p = byFreq ? byFreqSort : byWordId;
            words = wordRepository.findByJlpt(jlpt, p);

        } else {
            Pageable p = byFreq ? byFreqSort : byWordId;
            words = wordRepository.findAll(p);
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