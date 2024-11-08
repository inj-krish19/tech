package com.example.tech;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class EnvController {


    private Dotenv dotenv = Dotenv.load();
    // if want to access environment variable from system
    @Value("${WHATS_NAME:not working}")
    private String myEnvVariable;

    @GetMapping("/env")
    public String getEnvVariable() {
    	
    	if( myEnvVariable.equals("not working") ) {
    		myEnvVariable = dotenv.get("WHATS_NAME");
    	}
    	
        Path envFilePath = Paths.get(dotenv.get("DOTENV_PATH", ".env")).toAbsolutePath();
        System.out.println("Location of .env file: " + envFilePath);

        return "Value of WHATS_NAME: " + myEnvVariable;
    }
}
	