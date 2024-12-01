package com.example.tech;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;  // Corrected import for Model
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class SomeController {

    @Autowired
    private HttpSession session;

    @GetMapping("/somePage")
    public String somePage(Model model) {
        Integer authorId = (Integer) session.getAttribute("authorId");

        if (authorId != null) {
            // Do something with authorId, e.g., pass it to the view
            model.addAttribute("authorId", authorId);
        } else {
            // Optionally, you can add a message or perform some other action
            model.addAttribute("errorMessage", "Author not logged in.");
        }

        return "redirect:/";  // The name of the Thymeleaf view to render
    }
}
