package com.andrii.vaultnote.app.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class EmailVerificationConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
