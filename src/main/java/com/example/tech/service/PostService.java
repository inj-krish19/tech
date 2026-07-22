package com.example.tech.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.tech.model.Post;
import com.example.tech.repository.PostRepository;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    // Get all posts
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    // Get post by id
    public Optional<Post> getPostById(Long id) {
        return postRepository.findById(id);
    }

    // Create post
    public Post createPost(Post post) {
        return postRepository.save(post);
    }

    // Update post
    public Post updatePost(Long id, Post updatedPost) {
        return postRepository.findById(id)
                .map(post -> {
                    return postRepository.save(updatedPost);
                })
                .orElseThrow(() ->
                        new RuntimeException("Post not found with id " + id));
    }

    // Delete post
    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

}