package com.example.tech.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.tech.model.Blogger;
import com.example.tech.repository.BloggerRepository;

@Service
public class BloggerService {

    private final BloggerRepository bloggerRepository;

    public BloggerService(BloggerRepository bloggerRepository) {
        this.bloggerRepository = bloggerRepository;
    }

    // Get all bloggers
    public List<Blogger> getAllBloggers() {
        return bloggerRepository.findAll();
    }

    // Get blogger by id
    public Optional<Blogger> getBloggerById(Long id) {
        return bloggerRepository.findById(id);
    }

    // Create blogger
    public Blogger createBlogger(Blogger blogger) {
        return bloggerRepository.save(blogger);
    }

    // Update blogger
    public Blogger updateBlogger(Long id, Blogger updatedBlogger) {
        return bloggerRepository.findById(id)
                .map(blogger -> {
                    updatedBlogger.setAuthorId(id);
                    return bloggerRepository.save(updatedBlogger);
                })
                .orElseThrow(() ->
                        new RuntimeException("Blogger not found with id " + id));
    }

    // Delete blogger
    public void deleteBlogger(Long id) {
        bloggerRepository.deleteById(id);
    }
}