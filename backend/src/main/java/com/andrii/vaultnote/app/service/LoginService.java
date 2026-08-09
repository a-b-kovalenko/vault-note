package com.andrii.vaultnote.app.service;

import com.andrii.vaultnote.app.api.auth.dto.LoginRequest;

public interface LoginService {

  LoginResult login(LoginRequest request);

}
