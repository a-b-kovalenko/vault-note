package com.andrii.vaultnote.app.service;

import com.andrii.vaultnote.app.api.auth.dto.PasswordResetConfirmRequest;
import com.andrii.vaultnote.app.api.auth.dto.PasswordResetRequest;

public interface PasswordResetService {

  void requestPasswordReset(PasswordResetRequest request);

  void confirmPasswordReset(PasswordResetConfirmRequest request);

}
