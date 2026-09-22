package com.ensolution.ems.schedule.application.validator;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.port.out.CustomFieldDefinitionRepository;
import com.ensolution.ems.schedule.domain.custom_field.CustomFieldDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 커스텀 필드 정의의 비즈니스 규칙 검증. 단건 존재·소유권은 Adapter의 {@code findById(id, tenantId)}가,
 * 키 형식·예약어는 도메인({@code CustomFieldDefinition#requireValidKey})이 담당하므로 여기서 다루지 않는다.
 * <p>
 * {@code ScheduleValidator}에 넣지 않은 것은 애그리거트가 다르기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class CustomFieldDefinitionValidator {

	private final CustomFieldDefinitionRepository customFieldDefinitionRepository;

	/** 키는 tenant 안에서 유일해야 한다. 부모 aggregate가 없는 전역 유일 체크이므로 tenantId를 함께 받는다. */
	public void requireUniqueKey(String key, Long tenantId) {
		if (customFieldDefinitionRepository.existsByKeyAndTenantId(key, tenantId)) {
			throw new CustomException(ErrorCode.CUSTOM_FIELD_KEY_ALREADY_EXISTS);
		}
	}

	/**
	 * 회차 값으로 저장하려는 키가 전부 이 tenant에 정의돼 있어야 한다. 정의 목록이 곧 템플릿 검사의
	 * "알려진 커스텀 키"이므로, 미정의 키가 문서에 들어가면 검사가 "없다"고 한 이름이 실제로는 출력되는
	 * 모순이 생긴다. 어느 키가 문제인지는 요청마다 다르므로 메시지에 싣는다.
	 */
	public void requireDefinedKeys(Set<String> keys, Long tenantId) {
		if (keys == null || keys.isEmpty()) return;

		Set<String> defined = customFieldDefinitionRepository.findAll(tenantId).stream()
			.map(CustomFieldDefinition::getKey)
			.collect(Collectors.toSet());

		Set<String> undefined = keys.stream()
			.filter(key -> !defined.contains(key))
			.collect(Collectors.toCollection(TreeSet::new));

		if (!undefined.isEmpty()) {
			throw new CustomException(ErrorCode.CUSTOM_FIELD_NOT_DEFINED,
				ErrorCode.CUSTOM_FIELD_NOT_DEFINED.getMessage() + " (" + String.join(", ", undefined) + ")");
		}
	}
}
