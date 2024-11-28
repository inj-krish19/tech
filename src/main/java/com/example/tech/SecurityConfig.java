package com.example.tech;

import java.util.ArrayList;
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
                    .requestMatchers("/", "/login", "/register", "/css/**", "/js/**").permitAll()  // Public access for only these routes
                    .requestMatchers("/admin/**", "/data/**", "/description/**").hasRole("ADMIN")  // Admin-only routes
                    .anyRequest().authenticated()  // Any other routes require authentication
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate) {
        // Create admin user with specific credentials
        UserDetails admin = User.builder()
                .username("iamadmin")
                .password(passwordEncoder.encode("boss"))
                .roles("ADMIN")
                .build();

        // Load regular users from the database
        List<UserDetails> usersFromDB = jdbcTemplate.query(
            "SELECT username, password FROM Blogger",
            (rs, rowNum) -> User.builder()
                    .username(rs.getString("username"))
                    .password(rs.getString("password"))
                    .roles("USER")
                    .build()
        );

        List<UserDetails> allUsers = new ArrayList<>(usersFromDB);
        allUsers.add(admin);  // Add admin user to the list

        return new InMemoryUserDetailsManager(allUsers);
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
   