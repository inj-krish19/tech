package com.example.tech.service;

import com.example.tech.model.Blogger;
import com.example.tech.repository.BloggerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    // Get blogger by ID
    public Optional<Blogger> getBloggerById(Long id) {
        return bloggerRepository.findById(id);
    }

    // Create a new blogger
    public Blogger createBlogger(Blogger blogger) {
        return bloggerRepository.save(blogger);
    }

    // Update an existing blogger
    public Blogger updateBlogger(Long id, Blogger updatedBlogger) {
        return bloggerRepository.findById(id).map(blogger -> {
            /*	blogger.setName(updatedBlogger.getName());
            blogger.setEmail(updatedBlogger.getEmail());
            blogger.setUsername(updatedBlogger.getUsername());
            blogger.setBio(updatedBlogger.getBio());
            blogger.setPassword(updatedBlogger.getPassword());
            blogger.setSocialLinks(updatedBlogger.getSocialLinks());
            blogger.setProfilePicture(updatedBlogger.getProfilePicture());	*/
            return bloggerRepository.save(blogger);
        }).orElseThrow(() -> new RuntimeException("Blogger not found with id " + id));
    }

    // Delete a blogger
    public void deleteBlogger(Long id) {
        bloggerRepository.deleteById(id);
    }
}
