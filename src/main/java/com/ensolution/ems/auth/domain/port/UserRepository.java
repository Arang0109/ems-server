package com.ensolution.ems.auth.domain.port;

import com.ensolution.ems.auth.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
		
		/** tenant 범위 단건 조회. 미존재·타 tenant 모두 빈 값으로 돌려준다(리소스 존재 은닉). */
    Optional<User> findById(Long id, Long tenantId);

		List<User> findAll(Long tenantId);

    Optional<User> findByUsername(String username);

		boolean existsByUsername(String username);

		/** tenant 범위 삭제. 삭제된 행이 없으면 {@code false}. */
		boolean deleteById(Long id, Long tenantId);
}
