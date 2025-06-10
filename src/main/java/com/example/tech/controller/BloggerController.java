package com.example.tech.controller;

import com.example.tech.model.Blogger;
import com.example.tech.service.BloggerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class BloggerController {

    @Autowired
    private BloggerService bloggerService;

    // Get all bloggers
    @GetMapping("/api/bloggers")
    public List<Blogger> getAllBloggers() {
        return bloggerService.getAllBloggers();
    }

    // Get blogger by ID
    @GetMapping("/{id}")
    public Optional<Blogger> getBloggerById(@PathVariable Long id) {
        return bloggerService.getBloggerById(id);
    }

    // Create a new blogger
    /*	@PostMapping
    public Blogger createBlogger(@RequestBody Blogger blogger) {
        return bloggerService.createBlogger(blogger);
    }

    // Update an existing blogger
    @PutMapping("/{id}")
    public Blogger updateBlogger(@PathVariable Long id, @RequestBody Blogger updatedBlogger) {
        return bloggerService.updateBlogger(id, updatedBlogger);
    }

    // Delete a blogger
    @DeleteMapping("/{id}")
    public void deleteBlogger(@PathVariable Long id) {
        bloggerService.deleteBlogger(id);
    }	*/
}
