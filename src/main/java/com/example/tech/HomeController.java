package com.example.tech;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HomeController {

    @GetMapping("/login") // Maps to /login (http://localhost:8080/login)
    public String login() {
        return "mylogin"; // Login page
    }

    @GetMapping("/register") // Maps to /register (http://localhost:8080/register)
    public String register(Model model) {
        return "signup"; // Register page
    }
    
    @GetMapping("/create") // Maps to /create (http://localhost:8080/create)
    public String createPost(Model model) {
        return "create"; // Create post page
    }

//    @GetMapping("/bloggers")
//    public String showBloggers(Model model) {
//        List<Map<String, String>> bloggers = new ArrayList<>();
//
//        // Sample data for bloggers
//        for (int i = 1; i <= 5; i++) {
//            Map<String, String> blogger = new HashMap<>();
//            blogger.put("id", String.valueOf(i));
//            blogger.put("name", "Blogger " + i);
//            blogger.put("bio", "This is bio for Blogger " + i);
//            blogger.put("image", "blogger" + i + ".jpg"); // Example image names
//            bloggers.add(blogger);
//        }
//
//        model.addAttribute("bloggers", bloggers);
//        return "bloggers"; // This will render the bloggers.html template
//    }
    
    @GetMapping("/error")
    public String getError() {
    	return "error";
    }
    
    @GetMapping("/about")
    public String getAbout() {
    	return "about";
    }
    
    @GetMapping("/create_post")
    public String getPost() {
    	return "createPost";
    }
    
    @GetMapping("/profile")
    public String getProfile() {
    	return "profilemanagement";
    }
    
    @GetMapping("/signup")
    public String getSignup() {
    	return "signup";
    }
    
    @PostMapping("/create") // Handles form submission for creating a post
    public String savePost(Model model, String title, String body, String topic, boolean publish) {
        // Here you would handle saving the post to your database
        // For example, you could call a service to save the post
        
        return "redirect:/admin/posts"; 
    }
}
