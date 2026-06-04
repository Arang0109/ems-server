package com.ensolution.ems.auth.infrastructure.adapter;

import com.ensolution.ems.auth.application.port.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {
	
	private final StringRedisTemplate redisTemplate;
	
	@Override
	public void save(String username, String refreshToken, long durationMillis) {
		redisTemplate.opsForValue()
				.set("RT:" + username, refreshToken, durationMillis, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public String get(String username) {
		return redisTemplate.opsForValue().get("RT:" + username);
	}
	
	@Override
	public void delete(String username) {
		redisTemplate.delete("RT:" + username);
	}
}