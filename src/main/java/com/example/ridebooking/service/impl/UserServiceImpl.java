package com.example.ridebooking.service.impl;

import com.example.ridebooking.dto.RegisterRequest;
import com.example.ridebooking.entity.Role;
import com.example.ridebooking.entity.User;
import com.example.ridebooking.repository.UserRepository;
import com.example.ridebooking.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(RegisterRequest req) {
        if (repo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        User u = new User();
        u.setName(req.getName());
        u.setEmail(req.getEmail());
        u.setPhone(req.getPhone());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        Role role;
        try {
            role = Role.valueOf(req.getRole());
        } catch (Exception e) {
            role = Role.ROLE_RIDER;
        }
        u.setRole(role);
        return repo.save(u);
    }
}
