package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 측정 시점 장비 하나에 대응하는 엑셀 뷰(식별·이력 정보 + 유형별 사양).
 * 사양(spec)은 장비 유형별로 구조가 다르므로 평탄화해서 노출하며, 해당 유형이 아닌 필드는 null,
 * 목록 필드는 빈 리스트다(템플릿의 jx:each가 null에서 깨지지 않도록).
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class EquipmentExportView {

	private final String type;                    // 장비 유형 코드
	private final String typeLabel;               // 장비 유형 한글명
	private final String managementNumber;        // 관리번호
	private final String serialNumber;            // 제조번호
	private final String modelName;               // 모델명
	private final String equipmentName;           // 장비명
	private final String alias;                   // 별칭
	private final String manufacturer;            // 제조사
	private final Integer calibrationCycle;       // 교정 주기(개월)
	private final LocalDate lastCalibrationDate;  // 최종 교정일

	// 유형별 사양
	private final BigDecimal totalVolume;         // 채취기 총 용량 (입자상 채취기/가스 채취기/기타)
	private final BigDecimal orificeDeltaH;       // 오리피스 보정계수 △H@ (입자상 채취기)
	private final BigDecimal yd;                  // 건식가스미터 계수 (입자상 채취기)
	private final String pitotTubeType;           // 피토관 유형 코드
	private final String pitotTubeTypeLabel;      // 피토관 유형 한글명

	// 사양 목록 (jx:each 대상)
	private final List<PitotCoefficientExportView> coefficients;  // 피토관 계수표
	private final List<BigDecimal> nozzleDiameters;               // 보유 노즐경 목록
}
