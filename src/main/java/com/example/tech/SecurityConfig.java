package com.example.tech;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
// import io.github.cdimascio.dotenv.Dotenv;

@Configuration
public class SecurityConfig{

//    private final Dotenv dotenv = Dotenv.configure().load();
    // Fetch the password from the .env file with a fallback to "defaultpassword" if not found.
    // private String password = dotenv.get("PASSWORD", "pass");
    private String password = "pass";
    
    @Value("${spring.security.user.name:defaultuser}")
    private String username;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    	http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/", "/home", "/public/**", "/resources/**", "/css/**", "/js/**", "/images/**", "/signup", "/about").permitAll()
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
        UserDetails user = User.builder()
            .username(username)
            .password(passwordEncoder.encode(password))
            .roles("USER")
            .build();
        
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
    