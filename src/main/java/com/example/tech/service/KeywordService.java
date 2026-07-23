package com.example.tech.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.tech.model.Keyword;
import com.example.tech.repository.KeywordRepository;

@Service
public class KeywordService {

    private final KeywordRepository keywordRepository;

    public KeywordService(KeywordRepository keywordRepository) {
        this.keywordRepository = keywordRepository;
    }

    // Get all keywords
    public List<Keyword> getAllKeywords() {
        return keywordRepository.findAll();
    }

    // Get keyword by id
    public Optional<Keyword> getKeywordById(Long id) {
        return keywordRepository.findById(id);
    }

    // Create keyword
    public Keyword createKeyword(Keyword keyword) {
        return keywordRepository.save(keyword);
    }

    // Update keyword
    public Keyword updateKeyword(Long id, Keyword updatedKeyword) {
        return keywordRepository.findById(id)
                .map(keyword -> {
                    updatedKeyword.setKeywordId(id);
                    return keywordRepository.save(updatedKeyword);
                })
                .orElseThrow(() ->
                        new RuntimeException("Keyword not found with id " + id));
    }

    // Delete keyword
    public void deleteKeyword(Long id) {
        keywordRepository.deleteById(id);
    }
}