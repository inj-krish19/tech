package com.example.tech;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;	

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
// import io.github.cdimascio.dotenv.Dotenv;

@Configuration
public class SecurityConfig{

//    private final Dotenv dotenv = Dotenv.configure().load();
    // Fetch the password from the .env file with a fallback to "defaultpassword" if not found.
    // private String password = dotenv.get("PASSWORD", "pass");
    private String password = "pass";
    boolean login = false;
    private String tempuser = "";
    

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Value("${spring.security.user.name:defaultuser}")
    private String username;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/**", "/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()  // Public access for only these routes
                    .requestMatchers("/profile").hasRole("USER")
                    .requestMatchers("/admin/**", "/data/**", "/description/**").hasRole("ADMIN")  // Admin-only routes
                    .anyRequest().authenticated()  // Any other routes require authentication
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/enums", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            );

        return http.build();
    }
    
    @PostMapping("/login")
    public String isThisLogin(@RequestParam String un, @RequestParam String pass, Model model, HttpServletRequest request) {
        this.tempuser = un;

        // Query for the user's hashed password and authorId
        String sql = "SELECT authorId, password FROM Blogger WHERE username = ?";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, un);

        if (!results.isEmpty()) {
            Map<String, Object> row = results.get(0);  // Get the first result
            String hashed = (String) row.get("password");
            Integer userId = (Integer) row.get("authorId");

            if (passwordEncoder().matches(pass, hashed)) {
                // Set userId in session
                request.getSession().setAttribute("userId", userId);
                
                System.out.print(request.toString());
                
                this.login = true;

                model.addAttribute("success", "Login Successfully ...");
                return "redirect:/home";  // Redirect to home page or wherever after successful login
            }
        }

        // If no match, return to login page with an error message
        model.addAttribute("error", "Invalid username or password");
        return "login";  // Redirect back to login page on failure
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate, HttpServletRequest request) {
        // Create admin user with specific credentials
        UserDetails admin = User.builder()
                .username("iamadmin")
                .password(passwordEncoder.encode("boss"))
                .roles("ADMIN")
                .build();
        
        // Load regular users from the database
        List<UserDetails> usersFromDB = jdbcTemplate.query(
            "SELECT authorId, username, password FROM Blogger", 
            (rs, rowNum) -> {
                // Create a UserDetails object for each row returned by the query
                String username = rs.getString("username");
                String password = rs.getString("password");
                Integer authorId = rs.getInt("authorId"); // Get the authorId for session handling

                // Set the user in session if it's the first user or based on certain criteria
                if (admin == null && authorId != null) {
                    request.getSession().setAttribute("userId", authorId);  // Store authorId in session
                    System.out.print(request.getSession().getAttribute("userId"));
                }

                // Return a UserDetails object
                return User.builder()
                        .username(username)
                        .password(password)
                        .roles("USER")
                        .build();
            }
        );

        // Combine the admin user and regular users from the database
        List<UserDetails> allUsers = new ArrayList<>(usersFromDB);
        allUsers.add(admin);  // Add admin user to the list

        // Return an InMemoryUserDetailsManager with all users
        return new InMemoryUserDetailsManager(allUsers);
    }

    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
   