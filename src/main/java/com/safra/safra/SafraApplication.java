package com.safra.safra;

import com.safra.safra.entity.Role;
import com.safra.safra.entity.User;
import com.safra.safra.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@SpringBootApplication
public class SafraApplication {

	public static void main(String[] args) {
		SpringApplication.run(SafraApplication.class, args);
	}
}

