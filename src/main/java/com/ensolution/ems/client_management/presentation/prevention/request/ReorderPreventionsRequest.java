package com.ensolution.ems.client_management.presentation.prevention.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 방지설비 표시 순서 변경 요청.
 *
 * <p>{@code orderedIds} 는 이 측정지점의 방지설비 <b>전체</b>여야 한다. 배열 순서가 곧 표시 순위다.
 * 서버가 보유한 집합과 정확히 일치하지 않으면 — 빠진 id, 모르는 id, 중복 id 중 하나라도 있으면 —
 * 아무것도 저장하지 않고 거절한다.
 *
 * <p>"요청에 없는 항목은 서버 값을 유지"를 택하지 않은 이유: 순위는 집합 전체에 대한 전순서라
 * 부분 갱신하면 다른 항목과 값이 겹치거나 구멍이 생겨 순위 자체가 정의되지 않는다.
 * 내가 화면을 연 뒤 다른 사용자가 시설을 추가·삭제한 경우도 이 규칙이 함께 잡아낸다.
 */
public record ReorderPreventionsRequest(
	@NotNull
	Long stackId,

	@NotEmpty
	List<Long> orderedIds
) {
}
