package com.ensolution.ems.schedule.domain.sampling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이전 회차 기록지 재사용 규칙 검증.
 *
 * <p>지워야 할 값이 남으면 옛 값이 그대로 성적서로 나가고, 남겨야 할 값이 지워지면 불러오기 자체가
 * 쓸모없어진다. 양쪽 모두 이 테스트가 막는다.
 */
class SheetReuseTest {

	private static SamplingSheet fullSheet() {
		return SamplingSheet.builder()
			.category(MeasurementCategory.DUST)
			.version(7L)
			.weather(WeatherData.builder()
				.atmosphericPressure(new BigDecimal("1013"))
				.temperature(new BigDecimal("21.5"))
				.humidity(new BigDecimal("60"))
				.build())
			.exhaustGas(ExhaustGasData.builder()
				.o2Concentration(List.of(new BigDecimal("8.1")))
				.noxConcentration(List.of(new BigDecimal("45")))
				.gasAnalyzerStartTime(LocalTime.of(9, 0))
				.thcAnalyzerStartTime(LocalTime.of(9, 30))
				.o2CorrectionFactor(new BigDecimal("1.2"))
				.build())
			.moisture(MoistureData.builder()
				.suctionVelocity(new BigDecimal("10"))
				.moistureRatio(new BigDecimal("7.3"))
				.samplingStartTime(LocalTime.of(10, 0))
				.samplingEndTime(LocalTime.of(10, 30))
				.build())
			.particulateSampling(ParticulateSampling.builder()
				.thimbleFilter("원통여지 A")
				.totalDryGasVolume(new BigDecimal("1.5"))
				.samplingStartedAt(LocalTime.of(11, 0))
				.samplingEndedAt(LocalTime.of(11, 40))
				.build())
			.flowRate(FlowRateData.builder()
				.stackArea(new BigDecimal("0.785"))
				.appliedPitotCoefficient(new BigDecimal("0.84"))
				.standardDryGasFlowRate(new BigDecimal("12000"))
				.build())
			.samplingPoints(List.of(SamplingPoint.builder()
				.gasTemperature(new BigDecimal("150"))
				.dynamicPressure(new BigDecimal("3.2"))
				.staticPressure(new BigDecimal("-1.5"))
				.build()))
			.gaseousSamplings(List.of(GaseousSampling.builder()
				.sampleName("1차 시료")
				.sampleNumber("A-2025-001")
				.blankSampleNumber("BLK-2025-001")
				.samplingStartedAt(LocalTime.of(11, 0))
				.samplingEndedAt(LocalTime.of(11, 40))
				.suctionQuantity(new BigDecimal("500"))
				.build()))
			.samplingPointCount(4)
			.build();
	}

	@Nested
	@DisplayName("지우는 값 — 그 회차에만 유효한 것")
	class Cleared {

		@Test
		@DisplayName("시트 버전을 비운다 — 옛 버전을 되돌려 보내면 저장이 충돌로 거부된다")
		void clearsVersion() {
			assertThat(SheetReuse.forNextRound(fullSheet()).getVersion()).isNull();
		}

		@Test
		@DisplayName("기상 조건은 측정일의 날씨이므로 비운다 — 대기압은 예외로 남는다")
		void clearsWeatherExceptPressure() {
			WeatherData weather = SheetReuse.forNextRound(fullSheet()).getWeather();

			assertThat(weather.getTemperature()).isNull();
			assertThat(weather.getHumidity()).isNull();
			assertThat(weather.getAtmosphericPressureMmHg()).isNull();
		}

		@Test
		@DisplayName("대기압이 없던 회차는 기상 블록 자체를 비운다 — 남길 값이 없다")
		void clearsWeatherWhenPressureMissing() {
			SamplingSheet source = fullSheet().toBuilder()
				.weather(WeatherData.builder().temperature(new BigDecimal("21.5")).build())
				.build();

			assertThat(SheetReuse.forNextRound(source).getWeather()).isNull();
		}

		@Test
		@DisplayName("시료번호와 바탕시료번호를 비운다 — 복사되면 분석 의뢰가 섞인다")
		void clearsSampleNumbers() {
			GaseousSampling gaseousSampling = SheetReuse.forNextRound(fullSheet()).getGaseousSamplings().getFirst();

			assertThat(gaseousSampling.getSampleNumber()).isNull();
			assertThat(gaseousSampling.getBlankSampleNumber()).isNull();
		}

		@Test
		@DisplayName("유량 집계를 비운다 — 전량 계산 결과라 새 측정점 입력과 어긋난 값이 화면에 남는다")
		void clearsFlowRate() {
			assertThat(SheetReuse.forNextRound(fullSheet()).getFlowRate()).isNull();
		}

		@Test
		@DisplayName("채취 시각과 분석기 시작 시각을 비운다")
		void clearsTimes() {
			SamplingSheet reused = SheetReuse.forNextRound(fullSheet());

			assertThat(reused.getGaseousSamplings().getFirst().getSamplingStartedAt()).isNull();
			assertThat(reused.getGaseousSamplings().getFirst().getSamplingEndedAt()).isNull();
			assertThat(reused.getMoisture().getSamplingStartTime()).isNull();
			assertThat(reused.getMoisture().getSamplingEndTime()).isNull();
			assertThat(reused.getParticulateSampling().getSamplingStartedAt()).isNull();
			assertThat(reused.getParticulateSampling().getSamplingEndedAt()).isNull();
			assertThat(reused.getExhaustGas().getGasAnalyzerStartTime()).isNull();
			assertThat(reused.getExhaustGas().getThcAnalyzerStartTime()).isNull();
		}
	}

	@Nested
	@DisplayName("남기는 값 — 다음 회차의 출발점이 되는 것")
	class Kept {

		@Test
		@DisplayName("대기압을 남긴다 — 그날 날씨보다 굴뚝이 놓인 지역의 고도에 좌우되는 값이다")
		void keepsPressure() {
			assertThat(SheetReuse.forNextRound(fullSheet()).getWeather().getAtmosphericPressure())
				.isEqualByComparingTo("1013");
		}

		@Test
		@DisplayName("측정점 실측값을 남긴다 — 굴뚝 조건에 좌우되는 값이라 참고 가치가 있다")
		void keepsMeasuredPoints() {
			SamplingPoint point = SheetReuse.forNextRound(fullSheet()).getSamplingPoints().getFirst();

			assertThat(point.getGasTemperature()).isEqualByComparingTo("150");
			assertThat(point.getDynamicPressure()).isEqualByComparingTo("3.2");
			assertThat(point.getStaticPressure()).isEqualByComparingTo("-1.5");
		}

		@Test
		@DisplayName("시료 표기명은 남긴다 — 시설마다 굳어진 규칙이라 매번 다시 입력할 값이 아니다")
		void keepsSampleName() {
			GaseousSampling gaseousSampling = SheetReuse.forNextRound(fullSheet()).getGaseousSamplings().getFirst();

			assertThat(gaseousSampling.getSampleName()).isEqualTo("1차 시료");
			assertThat(gaseousSampling.getSuctionQuantity()).isEqualByComparingTo("500");
		}

		@Test
		@DisplayName("배출가스 농도와 수분 실측값을 남긴다")
		void keepsMeasuredValues() {
			SamplingSheet reused = SheetReuse.forNextRound(fullSheet());

			assertThat(reused.getExhaustGas().getO2Concentration()).hasSize(1);
			assertThat(reused.getExhaustGas().getNoxConcentration()).hasSize(1);
			assertThat(reused.getMoisture().getSuctionVelocity()).isEqualByComparingTo("10");
		}

		@Test
		@DisplayName("기록지 종류와 측정점 수, 소모품 표기를 남긴다")
		void keepsSheetIdentityAndConsumables() {
			SamplingSheet reused = SheetReuse.forNextRound(fullSheet());

			assertThat(reused.getCategory()).isEqualTo(MeasurementCategory.DUST);
			assertThat(reused.getSamplingPointCount()).isEqualTo(4);
			assertThat(reused.getParticulateSampling().getThimbleFilter()).isEqualTo("원통여지 A");
		}
	}

	@Nested
	@DisplayName("빈 값 — 없는 항목을 만들어내지 않는다")
	class Absent {

		@Test
		@DisplayName("시트가 없으면 null이다")
		void returnsNullForNullSheet() {
			assertThat(SheetReuse.forNextRound(null)).isNull();
		}

		@Test
		@DisplayName("비어 있던 하위 항목은 비운 채로 둔다")
		void keepsAbsentSectionsAbsent() {
			SamplingSheet minimal = SamplingSheet.builder()
				.category(MeasurementCategory.GAS)
				.version(3L)
				.build();

			SamplingSheet reused = SheetReuse.forNextRound(minimal);

			assertThat(reused.getVersion()).isNull();
			assertThat(reused.getExhaustGas()).isNull();
			assertThat(reused.getMoisture()).isNull();
			assertThat(reused.getParticulateSampling()).isNull();
			assertThat(reused.getGaseousSamplings()).isNull();
		}
	}

	@Test
	@DisplayName("원본 시트를 바꾸지 않는다 — 보관본이 손상되면 이전 회차 기록이 훼손된다")
	void doesNotMutateSource() {
		SamplingSheet source = fullSheet();

		SheetReuse.forNextRound(source);

		assertThat(source.getVersion()).isEqualTo(7L);
		assertThat(source.getWeather().getTemperature()).isEqualByComparingTo("21.5");
		assertThat(source.getGaseousSamplings().getFirst().getSampleNumber()).isEqualTo("A-2025-001");
	}
}
