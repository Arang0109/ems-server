package com.ensolution.ems.auth.application.validator;

import com.ensolution.ems.auth.domain.Role;
import com.ensolution.ems.auth.domain.port.RoleRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 사용자 애그리거트의 비즈니스 규칙 검증.
 * 단건 존재·tenant 소유권은 Adapter의 {@code findById(id, tenantId)}가 담당하므로 여기서 다루지 않는다.
 */
@Component
@RequiredArgsConstructor
public class UserValidator {

	private final RoleRepository roleRepository;

	/**
	 * 테넌트 범위 API로 부여할 수 있는 역할인지 확인한다.
	 * <p>
	 * {@code PLATFORM_ADMIN}은 특정 테넌트에 속하지 않는 전역 역할이라 테넌트 관리자가 부여할 수 없다.
	 * 이 검증이 없으면 관리자가 자기 계정의 역할을 운영자로 바꿔 권한을 상승시킬 수 있다.
	 * 운영자 계정은 {@code PlatformAdminInitializer}(부트스트랩)로만 만들어진다.
	 *
	 * @param roleId 부여하려는 역할. {@code null}이면 역할 변경이 없는 것으로 보고 통과시킨다
	 */
	public void requireAssignableRole(Long roleId) {
		if (roleId == null) {
			return;
		}
		// 존재하지 않는 roleId면 Adapter가 ROLE_NOT_FOUND를 던진다.
		Role role = roleRepository.findById(roleId);
		if (role.isPlatformAdmin()) {
			throw new CustomException(ErrorCode.ROLE_NOT_ASSIGNABLE);
		}
	}
}
