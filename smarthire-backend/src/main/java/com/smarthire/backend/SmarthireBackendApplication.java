package com.smarthire.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmarthireBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmarthireBackendApplication.class, args);
	}

}
