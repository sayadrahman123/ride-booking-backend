package com.example.ridebooking.service;


import com.example.ridebooking.dto.RegisterRequest;
import com.example.ridebooking.entity.User;

public interface UserService {
    User register(RegisterRequest req);
}
