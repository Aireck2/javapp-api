package com.aireck.javapp.service;

import com.aireck.javapp.dto.LoginRequest;
import com.aireck.javapp.dto.RefreshTokenRequest;
import com.aireck.javapp.dto.RegisterRequest;
import com.aireck.javapp.dto.TokenResponse;
import com.aireck.javapp.dto.UserResponse;

public interface AuthService {

  UserResponse register(RegisterRequest request);

  TokenResponse login(LoginRequest request);

  TokenResponse refresh(RefreshTokenRequest request);

  void logout(RefreshTokenRequest request);
}
