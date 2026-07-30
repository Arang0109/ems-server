package com.ensolution.ems.global.security.user;

import com.ensolution.ems.auth.infrastructure.entity.RoleEntity;
import com.ensolution.ems.auth.infrastructure.entity.UserEntity;
import com.ensolution.ems.auth.infrastructure.repository.UserJpaRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.platform.infrastructure.entity.TenantEntity;
import com.ensolution.ems.platform.infrastructure.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
  
  private final UserJpaRepository userRepository;
	private final TenantJpaRepository tenantJpaRepository;
  
  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) {
		UserEntity user = userRepository.findByUsername(username)
			.orElseThrow(() -> new UsernameNotFoundException("User not found : " + username));
		TenantEntity tenant = tenantJpaRepository.findByTenantId(user.getTenantId())
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
		
		Set<GrantedAuthority> authorities = new HashSet<>();
		RoleEntity role = user.getRole();
		
		authorities.add(
			new SimpleGrantedAuthority("ROLE_" + role.getName())
		);
		
		return new CustomUserDetails(
			user.getUserId(),
			user.getTenantId(),
			tenant.getName(),
			user.getUsername(),
			user.getPassword(),
			user.getName(),
			authorities
		);
  }
}
