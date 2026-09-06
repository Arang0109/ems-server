package com.ensolution.ems.auth.application.port.out;

public interface PasswordEncryptor {
	String encode(String rawPassword);
	boolean matches(String rawPassword, String encodedPassword);
}
