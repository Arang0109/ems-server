package com.ensolution.ems.auth.domain.port;

public interface PasswordEncryptor {
	String encode(String rawPassword);
	boolean matches(String rawPassword, String encodedPassword);
}
