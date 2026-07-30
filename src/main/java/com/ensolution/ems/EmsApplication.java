package com.ensolution.ems;

import com.ensolution.ems.global.security.domain.JwtProperties;
import com.ensolution.ems.storage.infrastructure.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableConfigurationProperties({JwtProperties.class, StorageProperties.class})
@EnableJpaAuditing
@SpringBootApplication
public class EmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmsApplication.class, args);
    }

}
