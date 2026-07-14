package com.ensolution.ems.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB 도큐먼트의 {@code @CreatedDate} / {@code @LastModifiedDate} 자동 세팅을 활성화합니다.
 * JPA auditing({@code @EnableJpaAuditing})과는 별개의 매핑 컨텍스트를 사용합니다.
 */
@Configuration
@EnableMongoAuditing
public class MongoAuditingConfig {
}
