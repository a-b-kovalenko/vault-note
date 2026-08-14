package com.andrii.vaultnote.app.service;

import com.andrii.vaultnote.app.api.auth.dto.RegisterUserRequest;
import com.andrii.vaultnote.app.api.auth.dto.RegisterUserResponse;
import jakarta.validation.Valid;

public interface RegistrationService {
  RegisterUserResponse registerUser(@Valid RegisterUserRequest request, String clientIp);
}
