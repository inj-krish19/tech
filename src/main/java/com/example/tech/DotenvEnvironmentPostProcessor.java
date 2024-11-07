package com.example.tech;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try{
            Dotenv dotenv = Dotenv.load();
            Map<String, Object> dotenvProperties = new HashMap<>();
            
            dotenv.entries().forEach(entry -> dotenvProperties.put(entry.getKey(), entry.getValue()));
            environment.getPropertySources().addLast(new MapPropertySource("dotenvProperties", dotenvProperties));
        }catch( Exception e ){
            System.out.println("Environment Variable File Not Found");
        }
    }
}
