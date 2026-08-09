package com.andrii.vaultnote.app.mail.impl;

import com.andrii.vaultnote.app.mail.MailMessage;
import com.andrii.vaultnote.app.mail.MailSender;
import com.andrii.vaultnote.app.config.MailProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SmtpMailSender implements MailSender {

  JavaMailSender javaMailSender;
  MailProperties mailProperties;

  @Override
  public void send(MailMessage message) {
    var mail = new SimpleMailMessage();
    mail.setFrom(mailProperties.from());
    mail.setTo(message.to());
    mail.setSubject(message.subject());
    mail.setText(message.text());

    javaMailSender.send(mail);
  }
}
