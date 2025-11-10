package com.safra.safra.controller;

import com.safra.safra.entity.Role;
import com.safra.safra.entity.User;
import com.safra.safra.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/admin")
public class AdminController {

	private final UserRepository userRepository;

	@Autowired
	public AdminController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	// Return all users (admin only)
	@GetMapping("/users")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<User>> getAllUsers() {
		List<User> users = userRepository.findAll();
		return ResponseEntity.ok(users);
	}

	// Get user by id
	@GetMapping("/users/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getUserById(@PathVariable long id) {
		Optional<User> userOpt = userRepository.findById(id);
		if (userOpt.isPresent()) {
			return ResponseEntity.ok(userOpt.get());
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
	}

	// Change user's role
	public static class RoleChangeRequest {
		public String role;
	}

	@PutMapping("/users/{id}/role")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> changeUserRole(@PathVariable long id, @RequestBody RoleChangeRequest req) {
		Optional<User> userOpt = userRepository.findById(id);
		if (userOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
		}
		User user = userOpt.get();
		if (req == null || req.role == null || req.role.isBlank()) {
			return ResponseEntity.badRequest().body("Missing role in request body");
		}
		try {
			Role newRole = Role.valueOf(req.role.trim().toUpperCase());
			user.setRole(newRole);
			userRepository.save(user);
			return ResponseEntity.ok(user);
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body("Invalid role: " + req.role);
		}
	}

	// Ban/unban user
	public static class BanRequest {
		public Boolean banned;
	}

	@PutMapping("/users/{id}/ban")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> setUserBan(@PathVariable long id, @RequestBody BanRequest req) {
		Optional<User> userOpt = userRepository.findById(id);
		if (userOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
		}
		if (req == null || req.banned == null) {
			return ResponseEntity.badRequest().body("Missing 'banned' boolean in request body");
		}
		User user = userOpt.get();
		user.setIsBanned(req.banned);
		userRepository.save(user);
		return ResponseEntity.ok(user);
	}

	// Verify user's email
	@PostMapping("/users/{id}/verify-email")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> verifyEmail(@PathVariable long id) {
		Optional<User> userOpt = userRepository.findById(id);
		if (userOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
		}
		User user = userOpt.get();
		user.setEmailVerified(true);
		userRepository.save(user);
		return ResponseEntity.ok(user);
	}

	// Delete user
	@DeleteMapping("/users/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteUser(@PathVariable long id) {
		Optional<User> userOpt = userRepository.findById(id);
		if (userOpt.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
		}
		userRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}

}
