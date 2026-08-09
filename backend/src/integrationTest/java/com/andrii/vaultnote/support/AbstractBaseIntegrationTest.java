package com.andrii.vaultnote.support;

import com.andrii.vaultnote.app.mail.MailSender;
import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.configuration.Orthography;
import com.github.database.rider.spring.api.DBRider;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DBRider
@DBUnit(schema = "vaultnote", caseInsensitiveStrategy = Orthography.LOWERCASE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AbstractBaseIntegrationTest.TestMailConfiguration.class)
public abstract class AbstractBaseIntegrationTest {

  protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
      .withDatabaseName("vault_note")
      .withUsername("user")
      .withPassword("password")
      .withInitScript("db/init.sql");

  static {
    POSTGRES.start();
  }

  @LocalServerPort
  protected int port;

  @DynamicPropertySource
  static void registerPostgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestMailConfiguration {

    @Bean
    @Primary
    MailSender testMailSender() {
      return Mockito.mock(MailSender.class);
    }
  }
}
