package com.andrii.vaultnote.app.service;

import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;

public interface EmailVerificationService {

  void issueVerificationEmail(UserEntity user);

  void verifyEmail(String token);

}
