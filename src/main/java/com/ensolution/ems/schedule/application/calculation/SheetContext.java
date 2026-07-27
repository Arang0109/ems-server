package com.ensolution.ems.schedule.application.calculation;

import com.ensolution.ems.equipment.domain.spec.NozzleSpec;
import com.ensolution.ems.equipment.domain.spec.PitotTubeSpec;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;
import com.ensolution.ems.schedule.application.command.StackData;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 측정 시트 계산 컨텍스트. 각 스텝이 순서대로 중간값을 축적하고, 마지막에 {@code ApplyResultStep}이
 * 결과를 시트에 반영한다. 계산에 필요한 외부 입력(측정시설 정보·피토관 계수·노즐경)은 생성 시 주입한다.
 */
@Getter
@Setter
public class SheetContext {

	private MeasurementSheet sheet;

	private final StackData stackData;

	private final List<PitotTubeSpec.PitotCoefficient> pitotCoefficients;
	private final List<NozzleSpec.NozzleDiameter> diameters;
	private final BigDecimal deltaH; // 오리피스 보정계수 (△H@, ParticleSampler spec)

	// 압력
	private BigDecimal Pa;    // 대기압 (mmHg)
	private BigDecimal Pg;    // 배출가스 절대압력 (mmHg)
	private BigDecimal Pm_g;      // 수분측정용 가스미터 게이지압 (mmHg)
	private BigDecimal Pm_g_inch; // 수분측정용 가스미터 게이지압 (inchH2O)

	// 수분
	private BigDecimal Xw;    // 수분량 (%)
	private BigDecimal Tm_g;  // 가스미터 흡입 가스온도 (°C)
	private BigDecimal Vm_g;  // 흡입 건조가스량 (L)
	private BigDecimal ma;    // 흡습 수분질량 (g)
	
	// 측정점관련
	private Integer samplingPointCnt;
	private BigDecimal area; // 측정시설 단면적 (m²)

	// 배출가스
	private BigDecimal o2;
	private BigDecimal co2;
	private BigDecimal co;
	private BigDecimal n2;
	private BigDecimal oxygenCorrectionFactor;
	private BigDecimal Md;    // 건조배출가스 분자량
	private BigDecimal Mw;    // 습윤배출가스 분자량

	// 밀도
	private BigDecimal standardGasDensity;
	private BigDecimal gasDensity;

	// 유속·유량
	private BigDecimal Cp;
	private BigDecimal Vs;               // 평균 유속 (m/s)
	private BigDecimal quantity;         // 현장 습윤 유량 (m³/h)
	private BigDecimal standardQuantity; // 표준상태 건조 유량 (Sm³/h)

	// 평균
	private BigDecimal avgTg; // 배출가스 절대온도 (K)
	private BigDecimal avgPv; // 배출가스 평균 동압
	private BigDecimal avgPs; // 배출가스 평균 정압
	private BigDecimal avgTm; // 가스미터 절대온도 (K)
	
	public SheetContext(
		MeasurementSheet sheet, StackData stackData,
		List<PitotTubeSpec.PitotCoefficient> pitotCoefficients,
		List<NozzleSpec.NozzleDiameter> diameters,
		BigDecimal deltaH
	) {
		this.sheet = sheet;
		this.stackData = stackData;
		this.pitotCoefficients = pitotCoefficients;
		this.diameters = diameters;
		this.deltaH = deltaH;
	}

	// 측정시설(굴뚝) 계산 입력 편의 접근자. 측정시설 스냅샷이 없으면 각 값은 null이다.
	public Shape getStackShape() {
		return stackData == null ? null : stackData.stackShape();
	}

	public BigDecimal getHeight() {
		return stackData == null ? null : stackData.height();
	}

	public BigDecimal getHorizontalLength() {
		return stackData == null ? null : stackData.horizontalLength();
	}

	public BigDecimal getVerticalLength() {
		return stackData == null ? null : stackData.verticalLength();
	}

	public Orientation getOrientation() {
		return stackData == null ? null : stackData.orientation();
	}

	public BigDecimal getStandardOxygen() {
		return stackData == null ? null : stackData.standardOxygen();
	}
}
