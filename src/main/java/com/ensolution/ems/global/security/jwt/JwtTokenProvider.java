package com.ensolution.ems.global.security.jwt;

import com.ensolution.ems.global.security.domain.JwtProperties;
import com.ensolution.ems.global.security.domain.JwtToken;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.security.user.CustomUserDetailsService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class JwtTokenProvider {
  private final long AT_VALID;
  private final long RT_VALID;
  private final SecretKey SECRET_KEY;
  
  private final CustomUserDetailsService customUserDetailsService;
  
  public JwtTokenProvider(
      JwtProperties jwtProperties,
      CustomUserDetailsService customUserDetailsService) {
    this.SECRET_KEY = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes());
    this.AT_VALID = jwtProperties.accessTokenValidity();
    this.RT_VALID = jwtProperties.refreshTokenValidity();
    this.customUserDetailsService = customUserDetailsService;
  }
  
  public JwtToken createToken(Authentication authentication) {
    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    String username = userDetails.getUsername();
    
    return new JwtToken(
        "Bearer",
        userDetails.getUsername(),
        userDetails.getName(),
        buildToken(username, AT_VALID),
        buildToken(username, RT_VALID));
  }
  
  public String createAccessToken(String username, String tenant, String role) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("tenant", tenant);
    claims.put("role", role);
    return buildToken(username, AT_VALID, claims);
  }

	public String createRefreshToken(String username) {
		return buildToken(username, RT_VALID);
	}
  
  public Authentication getAuthentication(String token) {
    String username = parseClaims(token).getSubject();
    UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
    
    return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
  }
  
  /**
   * 토큰의 주체(username)를 읽습니다. 서명 불일치·만료·형식 오류는 모두 빈 값입니다.
   * <p>
   * {@link #validateToken(String)} 후 다시 파싱하면 검증을 두 번 하게 되므로,
   * "유효하면 누구인지"가 필요한 곳에서는 이 메서드 하나만 씁니다.
   */
  public Optional<String> parseUsername(String token) {
    try {
      return Optional.ofNullable(parseClaims(token).getSubject());
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }
  
  public boolean validateToken(String token) {
    try {
      parseClaims(token); // 만료, 변조 여부 확인
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }
  
  private String buildToken(String username, long validity) {
    return buildToken(username, validity, Map.of());
  }

  private String buildToken(String username, long validity, Map<String, Object> claims) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + validity);

    return Jwts.builder()
        .claims(claims)                 // 커스텀 클레임 (tenantId, role 등)
        .subject(username)              // 사용자 식별자 (보통 username)
        .issuedAt(now)                  // 발급 시각
        .expiration(expiry)             // 만료 시각
        .signWith(SECRET_KEY)
        .compact();
  }
  
  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(SECRET_KEY)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}