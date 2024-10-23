package com.example.tech;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class BloggerController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/bloggers")
    public String getAllBloggers(Model model) {
        String sql = "SELECT * FROM blogger";
        List<Map<String, Object>> bloggers = jdbcTemplate.queryForList(sql);

        model.addAttribute("bloggers", bloggers);
        return "bloggers";
    }
    

    @GetMapping("/posts")
    public String showPosts(Model model) {
    	String sql = "SELECT * FROM posts";
        List<Map<String, Object>> posts = jdbcTemplate.queryForList(sql);

        model.addAttribute("posts", posts);
        return "show";
    }
    
    
}
