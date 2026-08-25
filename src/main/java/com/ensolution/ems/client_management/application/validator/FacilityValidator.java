package com.ensolution.ems.client_management.application.validator;

import com.ensolution.ems.client_management.domain.Facility;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 배출시설(Facility)의 비즈니스 규칙 검증을 담당한다.
 */
@Component
public class FacilityValidator {

	/**
	 * 순서 변경 요청이 이 측정지점의 배출시설 집합과 정확히 일치하는지 확인한다.
	 * <p>
	 * 중복·누락·미지의 id 를 저장 전에 한 번에 잡아 부분 저장을 막는다.
	 * 부분 목록을 허용하지 않는 이유는 순위가 집합 전체에 대한 전순서이기 때문이다 —
	 * 일부만 갱신하면 다른 항목과 값이 겹치거나 구멍이 생겨 순위 자체가 정의되지 않는다.
	 * 내가 화면을 연 뒤 다른 사용자가 시설을 추가·삭제한 경우도 이 규칙이 함께 잡아낸다.
	 * <p>
	 * 서비스가 이미 읽어온 목록과 대조하므로 포트를 재조회하지 않는다.
	 */
	public void requireExactOrder(List<Facility> current, List<Long> orderedIds) {
		if (Set.copyOf(orderedIds).size() != orderedIds.size()) {
			throw new CustomException(ErrorCode.FACILITY_ORDER_MISMATCH);
		}

		Set<Long> currentIds = current.stream()
			.map(Facility::getId)
			.collect(Collectors.toSet());

		if (!currentIds.equals(Set.copyOf(orderedIds))) {
			throw new CustomException(ErrorCode.FACILITY_ORDER_MISMATCH);
		}
	}
}
