package com.safra.safra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling // ADD THIS
public class SafraApplication {

	public static void main(String[] args) {
		SpringApplication.run(SafraApplication.class, args);
	}
}

