package com.ensolution.ems.auth.domain.port;

import com.ensolution.ems.auth.domain.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository {
    User save(User user);
		
    Optional<User> findById(Long id);
		
		List<User> findAll(Long tenantId);
    
    Optional<User> findByUsername(String username);

		boolean existsByUsername(String username);

		void deleteById(Long id);
}
