package com.ensolution.ems.global.security.user;

import com.ensolution.ems.auth.infrastructure.JpaUserEntity;
import com.ensolution.ems.auth.infrastructure.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
  
  private final JpaUserRepository userRepository;
  
  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) {
    JpaUserEntity user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found : " + username));
		
		return new CustomUserDetails(
				user.getId(),
				user.getUsername(),
				user.getPassword(),
				user.getName()
		);
  }
}
