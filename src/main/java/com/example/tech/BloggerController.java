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
    
    @PostMapping("/review")
    public String reviewPost(Model model, String title, String category, String description) {
        Map<String, Object> postData = new HashMap<>();
        postData.put("title", title);
        postData.put("category", category);
        postData.put("description", description);
        
        // Add the Map to the model
        model.addAttribute("post", postData);
        
        return "reviewPost"; // Assuming there's a Thymeleaf template named "show"
    }
    
    // @GetMapping("/review")
    // public String review(Model model, String title, String category, String description) {
    //     Map<String, Object> postData = new HashMap<>();
    //     postData.put("title", "title");
    //     postData.put("category", "category");
    //     postData.put("description", "description");
        
    //     // Add the Map to the model
    //     model.addAttribute("post", postData);
        
    //     return "reviewPost"; // Assuming there's a Thymeleaf template named "show"
    // }

    @GetMapping("/posts/{id}")
    public String viewPost(Model model, @PathVariable String id) {
        String sql = "SELECT * FROM posts WHERE id = ?";
        List<Map<String, Object>> post = jdbcTemplate.queryForList(sql, id);
        model.addAttribute("post", post);
        return "viewPost"; // Assuming there's a Thymeleaf template named "viewPost"
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
    	String sql = "SELECT * FROM posts where category like '%" + keyword + "%' or description like '%" + keyword + "%' or title like '%" + keyword + "%';";
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

    @GetMapping("/truncate")
    public String truncateTables(){
    	
    	String query = "truncate table posts cascade";
    	jdbcTemplate.execute(query);
    	
    	query = "truncate table blogger cascade";
    	jdbcTemplate.execute(query);
    	
    	return "show";
    }
    
    @GetMapping("/destroy")
    public String destroyTables(){

    	String query = "drop table posts";
    	jdbcTemplate.execute(query);
    	
    	query = "drop table blogger";
    	jdbcTemplate.execute(query);
    	
    	return "show";
    }
    
    @GetMapping("/create_tables")
    public String addTables() {
    	
    	String createBloggerTable = "CREATE TABLE IF NOT EXISTS blogger (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    name VARCHAR(100) NOT NULL\n" +
                ");";
        jdbcTemplate.execute(createBloggerTable);

        // Create posts table
        String createPostsTable = "CREATE TABLE IF NOT EXISTS posts (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    title VARCHAR(255) NOT NULL,\n" +
                "    category VARCHAR(100) NOT NULL,\n" +
                "    description TEXT NOT NULL,\n" +
                "    blogger_id INT REFERENCES blogger(id) ON DELETE CASCADE\n" +
                ");";
        jdbcTemplate.execute(createPostsTable);

        String addUniqueConstraint = "ALTER TABLE blogger ADD CONSTRAINT unique_name UNIQUE (name);";
        jdbcTemplate.execute(addUniqueConstraint);
        
        return "home";
        
    }
    
    
    @GetMapping("/insert_data")
    public String insertIntoTables() {
    
        // Insert bloggers
        String insertBloggers = "INSERT INTO blogger (name) VALUES\n" +
                "('KRISH'),\n" +
                "('DHRUV'),\n" +
                "('RAHIL'),\n" +
                "('VISHAL'),\n" +
                "('MOHIT')\n"; // Prevents duplicates
        jdbcTemplate.update(insertBloggers);

        // Insert posts
        String insertPosts = "INSERT INTO posts (title, category, description, blogger_id) VALUES\n" +
                "('First Blog Post', 'Coding', 'This is a description of the first blog post.', 1),\n" +
                "('Second Blog Post', 'Technology', 'This is a description of the second blog post.', 2),\n" +
                "('Third Blog Post', 'Robotics', 'This is a description of the third blog post.', 3),\n" +
                "('Fourth Blog Post', 'News', 'This is a description of the fourth blog post.', 4),\n" +
                "('Fifth Blog Post', 'Coding', 'This is a description of the fifth blog post.', 5)\n"; // Prevents duplicates
        jdbcTemplate.update(insertPosts); 
        
    	return "show";
    }
    
    
    

}