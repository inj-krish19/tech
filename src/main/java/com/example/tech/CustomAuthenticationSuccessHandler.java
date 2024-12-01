package com.example.tech;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private HttpSession session;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // Get the user details (assuming you have a custom UserDetails implementation)
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        // Assuming you have a custom UserDetails implementation with authorId
        Integer authorId = ((MyUserDetails) userDetails).getAuthorId(); // Get the authorId from the custom UserDetails

        // Store the authorId in the session
        session.setAttribute("authorId", authorId);

        // Redirect to the default page after successful login
        response.sendRedirect("/home");
    }
}
