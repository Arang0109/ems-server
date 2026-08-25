package com.ensolution.ems.equipment.domain;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 검사 실시 이력.
 * <p>
 * 장비와 생명주기가 다르고 건수가 계속 늘어나므로 {@link Equipment} 에 임베드하지 않고 별도 애그리거트로 둔다.
 * 한 번 기록되면 변경되지 않으므로 {@code update()} 를 두지 않는다(storage 의 DocumentVersion 과 동일).
 * <p>
 * 성적서 파일 첨부는 아직 범위 밖이다. MongoDB는 스키마리스라 추후 storage 모듈의 문서 id 필드를
 * 추가하는 것만으로 비파괴적으로 확장할 수 있으므로, 지금 쓰지 않는 필드를 API 계약에 미리 노출하지 않는다.
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class InspectionRecord {

	private String id;
	private Long tenantId;
	private String equipmentId;

	private InspectionType type;
	private LocalDate inspectedAt;

	/** 성적서에 명시된 유효기간 만료일. 장비 검사 항목의 예정일 지정으로 전달된다. */
	private LocalDate validUntil;

	private String agency;            // 검사·교정 기관
	private String certificateNumber; // 성적서 번호
	private InspectionResult result;
	private String remark;

	private LocalDateTime createdAt;

	public static InspectionRecord register(
		Long tenantId, String equipmentId, InspectionType type, LocalDate inspectedAt, LocalDate validUntil,
		String agency, String certificateNumber, InspectionResult result, String remark
	) {
		requireRecordable(type, inspectedAt);

		return InspectionRecord.builder()
			.tenantId(tenantId)
			.equipmentId(equipmentId)
			.type(type)
			.inspectedAt(inspectedAt)
			.validUntil(validUntil)
			.agency(agency)
			.certificateNumber(certificateNumber)
			.result(result)
			.remark(remark)
			.build();
	}

	/** 검사 종류와 실시일이 없으면 이력으로서 의미가 없다. 외부 의존이 없는 불변식이라 도메인이 소유한다. */
	private static void requireRecordable(InspectionType type, LocalDate inspectedAt) {
		if (type == null || inspectedAt == null) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
	}
}
