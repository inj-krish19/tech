package com.example.tech.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.*;

import com.example.tech.model.Keyword;
import com.example.tech.service.KeywordService;

@RestController
@RequestMapping("/api/keyword")
public class KeywordController {

    private final KeywordService keywordService;

    public KeywordController(KeywordService keywordService) {
        this.keywordService = keywordService;
    }

    @GetMapping("/")
    public List<Keyword> getAllKeywords() {
        return keywordService.getAllKeywords();
    }

    @GetMapping("/{id}")
    public Optional<Keyword> getKeywordById(@PathVariable Long id) {
        return keywordService.getKeywordById(id);
    }

    @PostMapping("/")
    public Keyword createKeyword(@RequestBody Keyword keyword) {
        return keywordService.createKeyword(keyword);
    }

    @PutMapping("/{id}")
    public Keyword updateKeyword(@PathVariable Long id,
                                 @RequestBody Keyword keyword) {
        return keywordService.updateKeyword(id, keyword);
    }

    @DeleteMapping("/{id}")
    public void deleteKeyword(@PathVariable Long id) {
        keywordService.deleteKeyword(id);
    }
}