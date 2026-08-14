package com.andrii.vaultnote.app.api.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.api.auth.dto.LoginRequest;
import com.andrii.vaultnote.app.service.EmailVerificationService;
import com.andrii.vaultnote.app.service.LoginService;
import com.andrii.vaultnote.app.service.PasswordResetService;
import com.andrii.vaultnote.app.service.RefreshTokenService;
import com.andrii.vaultnote.app.service.RegistrationService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class AuthControllerTest {

  static final String EMAIL = "user@example.com";

  @Mock
  EmailVerificationService emailVerificationService;
  @Mock
  LoginService loginService;
  @Mock
  PasswordResetService passwordResetService;
  @Mock
  RegistrationService registrationService;
  @Mock
  RefreshTokenService refreshTokenService;
  @Mock
  RefreshTokenCookieFactory refreshTokenCookieFactory;

  @InjectMocks
  AuthController authController;

  @Test
  void shouldPassClientIpToLoginService() {
    var servletRequest = new MockHttpServletRequest();
    servletRequest.setRemoteAddr("127.0.0.1");
    var loginRequest = LoginRequest.builder()
      .email(EMAIL)
      .password("Password1234")
      .build();
    var exception = new RuntimeException("login result is not needed in this test");
    when(loginService.login(loginRequest, "127.0.0.1")).thenThrow(exception);

    assertThatThrownBy(() -> authController.login(servletRequest, loginRequest))
      .isSameAs(exception);

    verify(loginService).login(loginRequest, "127.0.0.1");
  }
}
