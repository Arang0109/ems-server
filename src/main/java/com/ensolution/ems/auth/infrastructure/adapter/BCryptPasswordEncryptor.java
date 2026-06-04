package com.ensolution.ems.auth.infrastructure.adapter;

import com.ensolution.ems.auth.domain.port.PasswordEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BCryptPasswordEncryptor implements PasswordEncryptor {
	
	private final PasswordEncoder passwordEncoder;
	
	@Override
	public String encode(String rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}
	
	@Override
	public boolean matches(String rawPassword, String encodedPassword) {
		return passwordEncoder.matches(rawPassword, encodedPassword);
	}
}