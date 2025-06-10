package com.example.tech;

import java.io.IOException;
import java.security.Principal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
@Controller
public class HomeController {


    @Autowired
    private HttpSession session;
    
    String userExist = "";
	
    @GetMapping("/favicon.ico")
    public ResponseEntity<byte[]> getFavicon() throws IOException {
        ClassPathResource imgFile = new ClassPathResource("static/images/logo.jpg");
        byte[] bytes = StreamUtils.copyToByteArray(imgFile.getInputStream());

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType("image/x-icon"))
                .body(bytes);
    }

    
    @GetMapping("/login") // Maps to /login (http://localhost:8080/login)
    public String login(Model model, Principal principal) {
        
    	if (principal != null) {
            model.addAttribute("loggedInUser", userExist = principal.getName()); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
    	
        if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        
        return "mylogin"; // Login page
    }

    @GetMapping("/register") // Maps to /register (http://localhost:8080/register)
    public String register(Model model) {
    	if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        return "signup"; // Register page
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
    public String getError(Model model) {
    	if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
    	return "error";
    }
    
    @GetMapping("/about")
    public String getAbout(Model model) {
    	if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
    	return "about";
    }

    @GetMapping("/terms")
    public String terms(Model model) {
    	if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
    	return "terms";
    }

    @GetMapping("/write-for-us")
    public String writeForUs(Model model) {
    	if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
    	return "write-for-us";
    }

    @GetMapping("/contact")
    public String staticContact(Model model) {
    	if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
    	return "contact";
    }

    @GetMapping("/events")
    public String events(Model model) {
    	if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
    	return "events";
    }
    
    @GetMapping("/signup")
    public String getSignup(Model model) {
    	if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
    	return "signup";
    }
    
    @GetMapping("/home")
    public String getHome(Model model) {
    	if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
    	return "redirect:/";
    }
    
    @PostMapping("/create") // Handles form submission for creating a post
    public String savePost(Model model, String title, String body, String topic, boolean publish) {
        // Here you would handle saving the post to your database
        // For example, you could call a service to save the post
    	if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        return "redirect:/admin/posts"; 
    }
}
