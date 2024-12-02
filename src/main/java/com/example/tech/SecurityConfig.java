package com.example.tech;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**", "/").permitAll()
                .requestMatchers("/profile").hasRole("USER")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/secret", true)
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
    public String login(@RequestParam String username, @RequestParam String password, Model model, HttpServletRequest request) {
        String sql = "SELECT authorId, password FROM Blogger WHERE username = ?";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, username);

        if (!results.isEmpty()) {
            Map<String, Object> row = results.get(0);
            String storedHashedPassword = (String) row.get("password");
            Integer authorId = (Integer) row.get("authorId");

            if (passwordEncoder().matches(password, storedHashedPassword)) {
                request.getSession().setAttribute("userId", authorId);
                request.getSession().setAttribute("authorId", authorId);
                return "redirect:/login?success=true";
            }
        }
        model.addAttribute("error", "Invalid username or password");
        return "redirect:/login?error=true";
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        List<UserDetails> users = new ArrayList<>();
        users.add(new MyUserDetails("iamadmin", passwordEncoder.encode("boss"), List.of(new SimpleGrantedAuthority("ROLE_ADMIN")), null));
        jdbcTemplate.query("SELECT authorId, username, password FROM Blogger", 
            (rs, rowNum) -> users.add(new MyUserDetails(
                rs.getString("username"), 
                rs.getString("password"), 
                List.of(new SimpleGrantedAuthority("ROLE_USER")), 
                rs.getInt("authorId"))
            )
        );
        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
