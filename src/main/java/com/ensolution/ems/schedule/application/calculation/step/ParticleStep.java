package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.Calculator;
import com.ensolution.ems.schedule.application.calculation.SheetContext;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import com.ensolution.ems.schedule.domain.sheet.ParticleData;
import com.ensolution.ems.schedule.domain.sheet.SamplingPoint;
import com.ensolution.ems.schedule.domain.sheet.SamplingPoint.ParticleSampling;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 입자상 측정점의 등속흡인 관련 값을 계산한다. 측정점에 {@code particle}이 있는 경우에만 수행하며,
 * 측정점별로 kFactor → 오리피스차압(orificeDp) → 채취 건조가스량(Vm)·채취수분량(Vlc) → 등속흡입계수(isokineticRatio)를
 * 산출한다. 노즐사이즈 추천은 측정 전 계획 보조 기능이므로 프론트에만 두고, 여기서는 저장된 입력의 결정적 결과만 계산한다.
 */
@Component
@Order(8)
@RequiredArgsConstructor
public class ParticleStep implements SheetStep {

	private static final BigDecimal K = new BigDecimal("0.0000803989"); // 8.03989 × 10⁻⁵ (공정시험법 개정 시 변경)
	private static final BigDecimal DEFAULT_DELTA_H = BigDecimal.valueOf(46);
	private static final BigDecimal G2 = BigDecimal.valueOf(19.62); // 2 × 9.81
	private static final BigDecimal MMHG = BigDecimal.valueOf(13.6);
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
	private static final BigDecimal K273 = BigDecimal.valueOf(273);
	private static final BigDecimal TEN = BigDecimal.TEN;
	private static final BigDecimal PI = BigDecimal.valueOf(Math.PI);
	private static final BigDecimal VLC_COEF = BigDecimal.valueOf(1000);
	private static final BigDecimal MOISTURE_RATIO = BigDecimal.valueOf(18)
		.divide(BigDecimal.valueOf(22.4), 10, RoundingMode.HALF_UP); // 18/22.4
	private static final BigDecimal ISO_C1 = new BigDecimal("0.00346");
	private static final BigDecimal ISO_CONST = BigDecimal.valueOf(16670); // 1.667 × 10⁴
	private static final MathContext MC = new MathContext(10);
	private static final int SCALE = 10;

	private final Calculator calculator;

	@Override
	public void execute(SheetContext context) {
		MeasurementSheet sheet = context.getSheet();
		List<SamplingPoint> points = sheet.getSamplingPoints();
		if (points == null || points.isEmpty()) return;
		if (points.stream().noneMatch(p -> p.getParticle() != null)) return; // 입자상 없는 시트

		BigDecimal Cp = context.getCp();
		BigDecimal Xw = context.getXw();
		BigDecimal Md = context.getMd();
		BigDecimal Mw = context.getMw();
		BigDecimal Pa = context.getPa();
		BigDecimal Pg = context.getPg();
		BigDecimal gasDensity = context.getGasDensity();
		BigDecimal deltaH = context.getDeltaH() == null ? DEFAULT_DELTA_H : context.getDeltaH();

		List<SamplingPoint> computed = new ArrayList<>(points.size());
		List<BigDecimal> kFactors = new ArrayList<>();
		List<BigDecimal> orificeDps = new ArrayList<>();
		List<BigDecimal> isokineticRatios = new ArrayList<>();
		List<BigDecimal> vms = new ArrayList<>();
		List<BigDecimal> samplingTimes = new ArrayList<>();
		List<BigDecimal> tmRaws = new ArrayList<>();

		for (SamplingPoint p : points) {
			ParticleSampling ps = p.getParticle();
			if (ps == null) {
				computed.add(p);
				continue;
			}

			BigDecimal Ts = p.getTs();
			BigDecimal Pv = p.getPv();
			BigDecimal Ps = p.getPs();
			BigDecimal nozzleSize = ps.getNozzleSize();

			BigDecimal tmRaw = avgTm(ps);                             // (inTm + outTm) / 2
			BigDecimal Tm = tmRaw == null ? null : tmRaw.add(K273);   // 절대온도 K
			BigDecimal Tg = Ts == null ? null : Ts.add(K273);
			BigDecimal PgPoint = pgPoint(Pa, Ps);                     // 측정점별 절대압력 Pa + Ps/13.6

			BigDecimal kFactor = calculator.round(
				kFactor(Cp, deltaH, nozzleSize, Xw, Md, Mw, Tm, Tg, Pa, PgPoint), 2);
			BigDecimal orificeDp = kFactor == null || Pv == null ? null
				: calculator.round(kFactor.multiply(Pv), 2);
			BigDecimal Vm = calculator.round(vm(ps), 5);             // afterVm - beforeVm
			BigDecimal Vlc = calculator.round(vlc(Xw, Vm), 2);
			BigDecimal Vs = calculator.round(gasVelocity(Cp, Pv, gasDensity), 3);
			BigDecimal An = calculator.round(nozzleArea(nozzleSize), 3);
			BigDecimal isokineticRatio = calculator.round(
				isokineticRatio(Tg, Vlc, Vm, Pa, orificeDp, Tm, Pg, ps.getSamplingTime(), Vs, An), 1);

			ParticleSampling updatedPs = ps.toBuilder()
				.equipmentTemperature(withAvgTm(ps, tmRaw))
				.Vm(Vm)
				.Vlc(Vlc)
				.kFactor(kFactor)
				.orificeDp(orificeDp)
				.isokineticRatio(isokineticRatio)
				.build();

			computed.add(p.toBuilder().Vs(Vs).particle(updatedPs).build());

			addIfNotNull(kFactors, kFactor);
			addIfNotNull(orificeDps, orificeDp);
			addIfNotNull(isokineticRatios, isokineticRatio);
			addIfNotNull(vms, Vm);
			addIfNotNull(samplingTimes, ps.getSamplingTime());
			addIfNotNull(tmRaws, tmRaw);
		}

		ParticleData particle = (sheet.getParticle() == null ? ParticleData.builder() : sheet.getParticle().toBuilder())
			.avgKFactor(average(kFactors, 2))
			.avgOrificeDp(average(orificeDps, 2))
			.avgIsokineticRatio(average(isokineticRatios, 1))
			.totalVm(sum(vms, 5))
			.totalSamplingTime(sum(samplingTimes, 1))
			.build();

		context.setSheet(sheet.toBuilder()
			.samplingPoints(computed)
			.particle(particle)
			.build());

		// 가스미터 절대온도 avgTm = 평균((inTm+outTm)/2) + 273
		BigDecimal avgTmRaw = average(tmRaws, 1);
		if (avgTmRaw != null) context.setAvgTm(avgTmRaw.add(K273));
	}

	// kFactor = K × Cp² × △H × (nozzle×10)⁴ × (1 − Xw/100)² × (Md·Tm·Pg) / (Mw·Tg·Pa)
	private BigDecimal kFactor(
		BigDecimal Cp, BigDecimal deltaH, BigDecimal nozzleSize, BigDecimal Xw,
		BigDecimal Md, BigDecimal Mw, BigDecimal Tm, BigDecimal Tg, BigDecimal Pa, BigDecimal Pg) {
		if (anyNull(Cp, deltaH, nozzleSize, Xw, Md, Mw, Tm, Tg, Pa, Pg)) return null;
		if (Mw.signum() == 0 || Tg.signum() == 0 || Pa.signum() == 0) return null;

		BigDecimal nz4 = nozzleSize.multiply(TEN).pow(4);
		BigDecimal cp2 = Cp.multiply(Cp);
		BigDecimal moisture = BigDecimal.ONE.subtract(Xw.divide(HUNDRED, SCALE, RoundingMode.HALF_UP));
		BigDecimal moisture2 = moisture.multiply(moisture);
		BigDecimal frac = Md.multiply(Tm).multiply(Pg)
			.divide(Mw.multiply(Tg).multiply(Pa), SCALE, RoundingMode.HALF_UP);
		return K.multiply(cp2).multiply(deltaH).multiply(nz4).multiply(moisture2).multiply(frac);
	}

	// isokineticRatio = [Tg × (0.00346·Vlc + Vm·(Pm/Tm))] / [Pg × t × Vs × An] × 1.667×10⁴
	private BigDecimal isokineticRatio(
		BigDecimal Tg, BigDecimal Vlc, BigDecimal Vm, BigDecimal Pa, BigDecimal orificeDp,
		BigDecimal Tm, BigDecimal Pg, BigDecimal t, BigDecimal Vs, BigDecimal An) {
		if (anyNull(Tg, Vlc, Vm, Pa, orificeDp, Tm, Pg, t, Vs, An)) return null;
		if (Tm.signum() == 0 || Pg.signum() == 0 || t.signum() == 0 || Vs.signum() == 0 || An.signum() == 0) return null;

		BigDecimal Pm = Pa.add(orificeDp.divide(MMHG, SCALE, RoundingMode.HALF_UP));
		BigDecimal a = Tg.multiply(ISO_C1.multiply(Vlc).add(Vm.multiply(Pm.divide(Tm, SCALE, RoundingMode.HALF_UP))));
		BigDecimal b = Pg.multiply(t).multiply(Vs).multiply(An);
		return a.divide(b, SCALE, RoundingMode.HALF_UP).multiply(ISO_CONST);
	}

	// Vlc = Vm × 10³ × (Xw / (100 − Xw)) × (18/22.4)
	private BigDecimal vlc(BigDecimal Xw, BigDecimal Vm) {
		if (anyNull(Xw, Vm)) return null;
		BigDecimal denom = HUNDRED.subtract(Xw);
		if (denom.signum() == 0) return null;
		return Vm.multiply(VLC_COEF)
			.multiply(Xw.divide(denom, SCALE, RoundingMode.HALF_UP))
			.multiply(MOISTURE_RATIO);
	}

	// Vs = Cp × √(19.62 × Pv / gasDensity)
	private BigDecimal gasVelocity(BigDecimal Cp, BigDecimal Pv, BigDecimal gasDensity) {
		if (anyNull(Cp, Pv, gasDensity) || gasDensity.signum() <= 0) return null;
		BigDecimal value = G2.multiply(Pv).divide(gasDensity, 5, RoundingMode.HALF_UP);
		if (value.signum() < 0) return null;
		return Cp.multiply(value.sqrt(MC));
	}

	// 노즐 단면적 An = π × nozzle² / 4
	private BigDecimal nozzleArea(BigDecimal nozzleSize) {
		if (nozzleSize == null) return null;
		return PI.multiply(nozzleSize).multiply(nozzleSize).divide(BigDecimal.valueOf(4), SCALE, RoundingMode.HALF_UP);
	}

	// 측정점별 절대압력 Pg = Pa + Ps/13.6 (mmHg)
	private BigDecimal pgPoint(BigDecimal Pa, BigDecimal Ps) {
		if (anyNull(Pa, Ps)) return null;
		return Pa.add(Ps.divide(MMHG, SCALE, RoundingMode.HALF_UP));
	}

	// 채취 건조가스량 Vm = afterVm − beforeVm
	private BigDecimal vm(ParticleSampling ps) {
		ParticleSampling.EquipmentVolume ev = ps.getEquipmentVolume();
		if (ev == null || anyNull(ev.getAfterVm(), ev.getBeforeVm())) return null;
		return ev.getAfterVm().subtract(ev.getBeforeVm());
	}

	// (inTm + outTm) / 2
	private BigDecimal avgTm(ParticleSampling ps) {
		ParticleSampling.EquipmentTemperature et = ps.getEquipmentTemperature();
		if (et == null || anyNull(et.getInTm(), et.getOutTm())) return null;
		return et.getInTm().add(et.getOutTm()).divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
	}

	private ParticleSampling.EquipmentTemperature withAvgTm(ParticleSampling ps, BigDecimal avgTm) {
		ParticleSampling.EquipmentTemperature et = ps.getEquipmentTemperature();
		if (et == null) return null;
		return et.toBuilder().avgTm(calculator.round(avgTm, 1)).build();
	}

	private boolean anyNull(BigDecimal... values) {
		for (BigDecimal v : values) if (v == null) return true;
		return false;
	}

	private void addIfNotNull(List<BigDecimal> list, BigDecimal value) {
		if (value != null) list.add(value);
	}

	private BigDecimal average(List<BigDecimal> values, int scale) {
		if (values.isEmpty()) return null;
		BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		return sum.divide(BigDecimal.valueOf(values.size()), scale, RoundingMode.HALF_UP);
	}

	private BigDecimal sum(List<BigDecimal> values, int scale) {
		if (values.isEmpty()) return null;
		return calculator.round(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add), scale);
	}
}
