package com.example.tech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication
//@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class TechApplication {

	public static void main(String[] args) {
		try{
			SpringApplication.run(TechApplication.class, args);
		}catch( Exception e ){
			System.out.println("Some Error Occured");
		}
	}

}
