package com.andrii.vaultnote.app.mail;

public record MailMessage(
    String to,
    String subject,
    String text) {
}
