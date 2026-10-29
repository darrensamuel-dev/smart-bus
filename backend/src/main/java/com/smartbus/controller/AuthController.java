package com.smartbus.controller;

import com.smartbus.dto.Dto;
import com.smartbus.model.*;
import com.smartbus.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Demo-grade accounts: BCrypt-hashed passwords, role by inheritance.
 * (No JWT/session yet - see "Future work" in the README.)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Value("${smartbus.admin-invite-code}")
    private String adminInviteCode;

    public AuthController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @PostMapping("/register")
    public ResponseEntity<Dto.AuthResponse> register(@Valid @RequestBody Dto.RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new IllegalArgumentException("This email is already registered");
        }
        String hash = encoder.encode(req.password());
        User user;
        if ("ADMIN".equalsIgnoreCase(req.role())) {
            if (!adminInviteCode.equals(req.adminCode())) {
                throw new IllegalArgumentException("Invalid admin invite code");
            }
            user = new TransportAdmin(req.name(), req.email(), hash,
                    "EMP-" + ThreadLocalRandom.current().nextInt(10000, 99999), "Singanallur Depot");
        } else {
            user = new Passenger(req.name(), req.email(), hash,
                    "TIQ-" + ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L));
        }
        User saved = users.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PostMapping("/login")
    public Dto.AuthResponse login(@Valid @RequestBody Dto.LoginRequest req) {
        User user = users.findByEmail(req.email())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("Wrong email or password"));
        return toResponse(user);
    }

    private Dto.AuthResponse toResponse(User u) {
        return new Dto.AuthResponse(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
