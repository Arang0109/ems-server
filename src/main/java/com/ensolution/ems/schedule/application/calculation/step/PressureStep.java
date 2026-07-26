package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.SheetContext;
import com.ensolution.ems.schedule.domain.sheet.MoistureData;
import com.ensolution.ems.schedule.domain.sheet.WeatherData;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 대기압(hPa→mmHg), 가스미터 게이지압, 배출가스 절대압력 Pg를 계산한다. */
@Component
@Order(2)
public class PressureStep implements SheetStep {

	@Override
	public void execute(SheetContext context) {
		setAtmospherePressure(context);
		setGasMeterGaugePressure(context);
		setPg(context);
	}

	// Pa (mmH2O -> mmHg) 변환 후 Pa에 저장 (대기압)
	private void setAtmospherePressure(SheetContext context) {
		WeatherData weather = context.getSheet().getWeather();
		if (weather == null || weather.getPressure() == null) return;
		context.setPa(convertHpaToMmHg(weather.getPressure()));
	}

	// Pm_g (mmH2O -< mmHg) 변환 후 Pm_g에 저장 (가스미터게이지압)
	private void setGasMeterGaugePressure(SheetContext context) {
		MoistureData moisture = context.getSheet().getMoisture();
		if (moisture == null || moisture.getGasMeterGaugePressure() == null) return;
		context.setPm_g(convertMmH2OToMmHg(moisture.getGasMeterGaugePressure()));
	}

	// Pg (대기압 + (정압/13.6)) (배출가스 절대압력 mmHg)
	private void setPg(SheetContext context) {
		if (context.getPa() == null || context.getAvgPs() == null) return;
		context.setPg(context.getPa().add(convertMmH2OToMmHg(context.getAvgPs())));
	}

	// hPa → mmHg
	private BigDecimal convertHpaToMmHg(BigDecimal value) {
		return value
			.multiply(BigDecimal.valueOf(760.0))
			.divide(BigDecimal.valueOf(1013.25), 1, RoundingMode.HALF_UP);
	}

	// mmH2O → mmHg
	private BigDecimal convertMmH2OToMmHg(BigDecimal value) {
		return value.divide(BigDecimal.valueOf(13.6), 2, RoundingMode.HALF_UP);
	}
}
