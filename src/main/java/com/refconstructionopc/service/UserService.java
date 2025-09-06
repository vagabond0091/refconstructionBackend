package com.refconstructionopc.service;

import com.refconstructionopc.dto.LoginRequestDTO;
import com.refconstructionopc.dto.RegisterRequestDTO;

public interface UserService {
    String login(LoginRequestDTO request);
    String register(RegisterRequestDTO request);
}
