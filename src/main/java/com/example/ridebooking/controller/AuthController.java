package com.example.ridebooking.controller;

import com.example.ridebooking.dto.AuthResponse;
import com.example.ridebooking.dto.RegisterRequest;
import com.example.ridebooking.entity.User;
import com.example.ridebooking.repository.UserRepository;
import com.example.ridebooking.security.JwtProvider;
import com.example.ridebooking.service.UserService;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authManager;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    public AuthController(UserService userService,
                          AuthenticationManager authManager,
                          JwtProvider jwtProvider,
                          UserRepository userRepository) {
        this.userService = userService;
        this.authManager = authManager;
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        User created = userService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created.getId());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody com.example.ridebooking.dto.LoginRequest req) {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword());
        try {
            Authentication auth = authManager.authenticate(token);
            UserDetails ud = (UserDetails) auth.getPrincipal();
            String jwt = jwtProvider.generateToken(ud.getUsername());
            return ResponseEntity.ok(new AuthResponse(jwt));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }
}
