package com.example.tech;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
public class BloggerController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static Integer start = 1;
    

    @GetMapping("/") // Maps to the root URL (http://localhost:8080/)
    public String home(Model model) {

        List<String> topics = List.of("News","Coding","Robotics","Technology");
    	model.addAttribute("topics", topics);
        String sql = "SELECT * FROM posts limit 5";
        List<Map<String, Object>> posts = jdbcTemplate.queryForList(sql);
        model.addAttribute("posts", posts);
        return "home"; // Home page
    }
    
    // Get all bloggers
    @GetMapping("/bloggers")
    public String getAllBloggers(Model model) {
        String sql = "SELECT * FROM blogger";
        List<Map<String, Object>> bloggers = jdbcTemplate.queryForList(sql);
        model.addAttribute("bloggers", bloggers);
        return "bloggers";
    }
    
    // Show all posts
    @GetMapping("/posts")
    public String showPosts(Model model) {
        String sql = "SELECT * FROM posts";
        List<Map<String, Object>> posts = jdbcTemplate.queryForList(sql);
        model.addAttribute("posts", posts);
        return "show"; // Assuming there's a Thymeleaf template named "show"
    }
    
    @GetMapping("/filter/{category}")
    public String filterPosts(Model model, @PathVariable String category) {
        String sql = "SELECT * FROM posts where category='" + category + "';";
        List<Map<String, Object>> posts = jdbcTemplate.queryForList(sql);
        model.addAttribute("posts", posts);
        model.addAttribute("topic", category);
        return "filter"; // Assuming there's a Thymeleaf template named "show"
    }

    @PostMapping("/search")
    public String seacrhKeyword(Model model, String keyword ) {
        String sql = "SELECT * FROM posts where category='%" + keyword + "%' or description='%" + keyword + "%' or title='%" + keyword + "%';";
        List<Map<String, Object>> posts = jdbcTemplate.queryForList(sql);
        model.addAttribute("posts", posts);
        model.addAttribute("topic", keyword);
        return "filter"; // Assuming there's a Thymeleaf template named "show"
    }
    
    // Create a new post
    @PostMapping("/doPost")
    public String makePost(Model model, String title, String category, String description) {
        // SQL to insert a post into the database

    	String sql = "SELECT COUNT(*) FROM posts";
    	start = jdbcTemplate.queryForObject(sql, Integer.class);

    	start ++;
    	sql = "INSERT INTO posts(id, title, category, description) VALUES (?, ?, ?, ?)";

        // Remove HTML tags from description and trim it
        description = description.replaceAll("<[^>]*>", "").trim();
        
        // Execute the insert operation
        int rowsAffected = jdbcTemplate.update(sql, start, title, category, description);
        
        if (rowsAffected > 0) {
            // Redirect to the posts page after successful insertion
            return "redirect:/posts"; // Redirect to the posts list
        } else {
            // Handle failure (optional)
            model.addAttribute("error", "Failed to create post.");
            return "create_post"; // Assuming this is your creation page
        }
    }
    
    @GetMapping("/load-more-posts")
    @ResponseBody
    public Map<String, Object> loadMorePosts(@RequestParam("page") int page) {
        int pageSize = 5; // Number of posts per page
        int offset = (page - 1) * pageSize;

        // SQL query to fetch posts based on the page and pageSize
        String sql = "SELECT * FROM posts ORDER BY id LIMIT ? OFFSET ?";
        List<Map<String, Object>> posts = jdbcTemplate.queryForList(sql, pageSize, offset);

        // Check if there are more posts to load
        boolean hasMore = posts.size() == pageSize;

        // Prepare the response map
        Map<String, Object> response = new HashMap<>();
        response.put("posts", posts);
        response.put("hasMore", hasMore);

        return response;
    }


}