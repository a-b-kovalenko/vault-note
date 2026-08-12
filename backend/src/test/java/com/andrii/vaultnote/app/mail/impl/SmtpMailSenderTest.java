package com.andrii.vaultnote.app.mail.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.andrii.vaultnote.app.config.MailProperties;
import com.andrii.vaultnote.app.mail.MailMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpMailSenderTest {

  private static final String FROM = "no-reply@vaultnote.local";

  @Mock
  JavaMailSender javaMailSender;

  @Test
  void shouldMapMessageToSmtpMail() {
    var sender = new SmtpMailSender(javaMailSender, new MailProperties(FROM));
    var message = new MailMessage(
      "user@example.com",
      "Verify your email",
      "Open the verification link.");

    sender.send(message);

    var mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(javaMailSender).send(mailCaptor.capture());

    var sentMail = mailCaptor.getValue();
    assertThat(sentMail.getFrom()).isEqualTo(FROM);
    assertThat(sentMail.getTo()).containsExactly("user@example.com");
    assertThat(sentMail.getSubject()).isEqualTo("Verify your email");
    assertThat(sentMail.getText()).isEqualTo("Open the verification link.");
  }
}
