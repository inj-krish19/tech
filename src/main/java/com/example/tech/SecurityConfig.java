package com.example.tech;

import java.util.List;
import java.util.Map;

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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
// import io.github.cdimascio.dotenv.Dotenv;

@Configuration
public class SecurityConfig{

//    private final Dotenv dotenv = Dotenv.configure().load();
    // Fetch the password from the .env file with a fallback to "defaultpassword" if not found.
    // private String password = dotenv.get("PASSWORD", "pass");
    private String password = "pass";
    

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Value("${spring.security.user.name:defaultuser}")
    private String username;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    	http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/", "/home", "/public/**", "/resources/**", "/css/**", "/js/**", "/images/**", "/about").permitAll()
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .permitAll()
            .defaultSuccessUrl("/", true)
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout")
            .permitAll()
        )
        .csrf(csrf -> csrf.disable());  // Disable CSRF for development; enable in production

    return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        
    	String sql = "SELECT * FROM blogger";
        List<Map<String, Object>> bloggers = jdbcTemplate.queryForList(sql);
        
        List<UserDetails> users = jdbcTemplate.query(sql, (rs, rowNum) -> 
        User.builder()
	            .username(rs.getString("username"))
	            .password(passwordEncoder.encode("password"))
	            .roles("USER")
	            .build()
	    );
        
        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @PostMapping("/register")
    public String registerBlogger(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("bio") String bio,
            Model model) {

        // Check if username or email already exists
        String checkUserSql = "SELECT COUNT(*) FROM blogger WHERE username = ? OR email = ?";
        int count = jdbcTemplate.queryForObject(checkUserSql, Integer.class, username, email);

        if (count > 0) {
            model.addAttribute("error", "Username or Email already exists!");
            return "register"; // Return to the registration page if user already exists
        }


		// Encrypt the password before storing it
        String encodedPassword = passwordEncoder().encode(password);

        // Insert the new blogger into the database
        String insertSql = """
            INSERT INTO blogger (username, email, password, bio, created_at, updated_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """;
        int rowsAffected = jdbcTemplate.update(insertSql, username, email, encodedPassword, bio);

        if (rowsAffected > 0) {
            model.addAttribute("success", "Registration successful! Please login.");
            return "login"; // Redirect to the login page after successful registration
        } else {
            model.addAttribute("error", "Registration failed. Please try again.");
            return "register"; // Return to the registration page on failure
        }
    }
    
}
   