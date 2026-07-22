package com.example.tech.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.*;

import com.example.tech.model.Blogger;
import com.example.tech.service.BloggerService;

@RestController
@RequestMapping("/api/blogger")
public class BloggerController {

    private final BloggerService bloggerService;

    public BloggerController(BloggerService bloggerService) {
        this.bloggerService = bloggerService;
    }

    @GetMapping("/")
    public List<Blogger> getAllBloggers() {
        return bloggerService.getAllBloggers();
    }

    @GetMapping("/{id}")
    public Optional<Blogger> getBloggerById(@PathVariable Long id) {
        return bloggerService.getBloggerById(id);
    }

    @PostMapping("/")
    public Blogger createBlogger(@RequestBody Blogger blogger) {
        return bloggerService.createBlogger(blogger);
    }

    @PutMapping("/{id}")
    public Blogger updateBlogger(@PathVariable Long id,
                                 @RequestBody Blogger blogger) {
        return bloggerService.updateBlogger(id, blogger);
    }

    @DeleteMapping("/{id}")
    public void deleteBlogger(@PathVariable Long id) {
        bloggerService.deleteBlogger(id);
    }
}