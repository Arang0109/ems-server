package com.ensolution.ems.client_management.domain;

import com.ensolution.ems.global.common.enums.SampleGrouping;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객사가 측정물질에 쓰는 <b>측정방법</b>(채취 매체·방식). tenant 소유 애그리거트다.
 *
 * <p>한때 전역 enum이었다. enum으로는 "카트리지 항목은 한 번의 채취로 전부 함께 잡는다"는 사실과
 * 그에 딸린 값(표준 채취시간·통칭 시료명)을 둘 곳이 없어, 채취시간을 {@code pollutants}에 두면
 * 카트리지 항목마다 같은 값을 반복 저장하고 바꿀 때마다 동기화해야 했다. 채취시간은 물질이 아니라
 * 측정방법에 종속되는 값이므로(이행 종속) 측정방법을 애그리거트로 승격해 그 값들을 여기 둔다.
 * {@code Pollutant}는 {@code methodId}로 참조만 하므로 여기서 값을 바꾸면 그 방법을 쓰는 항목 전부에
 * 즉시 반영된다 — 동기화 경로가 없는 것이 이 구조의 요점이다.
 *
 * <p>측정 시점 스냅샷({@code schedule})은 이 값을 <b>복사</b>해 간다. 과거 회차의 기록지가 원장 변경에
 * 흔들리지 않아야 하기 때문이다.
 *
 * <ul>
 *   <li>{@code sampleGrouping} — 채취 단위. 가스상 시료 표에 행을 어떻게 적는지를 정한다.</li>
 *   <li>{@code mergedSampleName} — {@link SampleGrouping#MERGED}일 때 기록지에 적는 통칭명({@code VOCs}).
 *       불변식: 통칭명이 있으면 반드시 MERGED이고, MERGED면 반드시 통칭명이 있다.</li>
 *   <li>{@code samplingMinutes} — 표준(계획) 채취시간(분). 회차별 실측 시각은 schedule이 따로 갖는다.</li>
 * </ul>
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class MeasurementMethod {
	private Long id;
	private Long tenantId;
	private String name;
	private SampleGrouping sampleGrouping;
	private String mergedSampleName;
	private Integer samplingMinutes;
	private Integer sortOrder;

	public static MeasurementMethod register(
		Long tenantId,
		String name,
		SampleGrouping sampleGrouping,
		String mergedSampleName,
		Integer samplingMinutes,
		Integer sortOrder
	) {
		MeasurementMethod method = MeasurementMethod.builder()
			.tenantId(tenantId)
			.name(name)
			.sampleGrouping(sampleGrouping)
			.mergedSampleName(normalize(mergedSampleName))
			.samplingMinutes(samplingMinutes)
			.sortOrder(sortOrder)
			.build();
		method.requireConsistentGrouping();
		return method;
	}

	/**
	 * 수정한다. {@code name}·{@code sampleGrouping}은 null이면 기존 값을 유지한다.
	 *
	 * <p>{@code mergedSampleName}·{@code samplingMinutes}는 <b>전달값을 그대로 채택</b>한다. 둘 다 "없음"이
	 * 유효한 값이라 null을 "유지"로 읽으면 한번 채운 값을 비울 방법이 없어진다
	 * ({@code StackPollutant#update}의 허용기준과 같은 판단이다). 통칭 채취를 항목별 채취로 바꾸는 요청은
	 * 그래서 {@code sampleGrouping=PER_ITEM, mergedSampleName=null}을 함께 보내야 한다.
	 */
	public MeasurementMethod update(
		String name,
		SampleGrouping sampleGrouping,
		String mergedSampleName,
		Integer samplingMinutes
	) {
		MeasurementMethod updated = this.toBuilder()
			.name(keep(name, this.name))
			.sampleGrouping(keep(sampleGrouping, this.sampleGrouping))
			.mergedSampleName(normalize(mergedSampleName))
			.samplingMinutes(samplingMinutes)
			.build();
		updated.requireConsistentGrouping();
		return updated;
	}

	/** 통칭명은 MERGED에만, MERGED에는 반드시 통칭명이 있어야 한다. 외부 의존이 없는 도메인 불변식이다. */
	public void requireConsistentGrouping() {
		boolean merged = sampleGrouping == SampleGrouping.MERGED;
		boolean named = mergedSampleName != null;
		if (merged != named) {
			throw new CustomException(ErrorCode.MEASUREMENT_METHOD_GROUPING_MISMATCH);
		}
	}

	private static String normalize(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}

	private static <T> T keep(T value, T original) {
		return value == null ? original : value;
	}
}
