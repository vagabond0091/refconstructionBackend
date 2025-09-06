package com.refconstructionopc.service.serviceImpl;

import com.refconstructionopc.auth.security.service.JwtService;
import com.refconstructionopc.dto.LoginRequestDTO;
import com.refconstructionopc.dto.RegisterRequestDTO;
import com.refconstructionopc.dto.UserDTO;
import com.refconstructionopc.enums.Role;
import com.refconstructionopc.model.User;
import com.refconstructionopc.repository.UserRepository;
import com.refconstructionopc.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private final AuthenticationManager authenticationManager;
    @Autowired
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final JwtService jwtService;

    public UserServiceImpl(AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, UserRepository userRepository, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public String login(LoginRequestDTO request) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                request.getEmail(), request.getPassword());
        authenticationManager.authenticate(auth);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        UserDTO userDTO = convertUserEntityToDTO(user);
        return jwtService.generateToken(userDTO);
    }

    @Override
    public String register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase().trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Role must be ADMIN or EMPLOYEE");
        }
        User user = new User();
        user.setEmail(request.getEmail().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // hash password
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setRole(role);
        user = userRepository.save(user);

        UserDTO userDTO = convertUserEntityToDTO(user);

        return jwtService.generateToken(userDTO);
    }

    public UserDTO convertUserEntityToDTO(User user){
        UserDTO userDTO = new UserDTO();
        userDTO.setEmail(user.getEmail());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setRole(user.getRole());
        return userDTO;
    }
}
