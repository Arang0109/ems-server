package com.ensolution.ems.global.security.user;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {
	
	private final Long userId;
	private final String username;
	private final String password;
	private final String name;
	
	public CustomUserDetails(
			Long userId,
			String username,
			String password,
			String name
	) {
		this.userId = userId;
		this.username = username;
		this.password = password;
		this.name = name;
	}
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.emptyList();
	}
}