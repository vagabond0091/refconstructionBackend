package com.refconstructionopc.controller;

import com.refconstructionopc.dto.LoginRequestDTO;
import com.refconstructionopc.dto.RegisterRequestDTO;
import com.refconstructionopc.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class UserController {
    @Autowired
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(value = "/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDTO request) {
        try {
            String token = userService.login(request);
            return ResponseEntity.ok(token); // plain token in response body
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed: " + e.getMessage());
        }

    }
    @PostMapping(value = "/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequestDTO request) {
        try {
            String token = userService.register(request);
            return ResponseEntity.ok(token); // plain token in response body
        } catch (IllegalArgumentException e) {
            // Invalid credentials
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed: " + e.getMessage());
        }
    }
    @GetMapping(value = "/get")
    public String getAll() {
        return "Hello World";
    }
}
