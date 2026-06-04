package com.ensolution.ems.global.security.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="jwt")
public record JwtProperties(
    String secret,
    long accessTokenValidity,
    long refreshTokenValidity
) {
}
