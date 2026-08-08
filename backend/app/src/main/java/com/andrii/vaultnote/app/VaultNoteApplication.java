package com.andrii.vaultnote.app;

import com.andrii.vaultnote.users.infrastructure.persistence.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.UserJpaRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.andrii.vaultnote")
@EntityScan(basePackageClasses = UserEntity.class)
@EnableJpaRepositories(basePackageClasses = UserJpaRepository.class)
public class VaultNoteApplication {

    static void main(String[] args) {
        SpringApplication.run(VaultNoteApplication.class, args);
    }
}
