package com.ensolution.ems.schedule.domain.snapshot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실험실 분석 결과의 필드 소유 규칙 검증.
 *
 * <p><b>이 모듈에서 가장 중요한 불변식 중 하나다.</b> 실험·분석 탭과 성적서 탭이 한 항목의 필드를
 * 나눠 소유하고, 실험분석정보가 측정 시트와 같은 문서에 저장되므로 두 탭이 같은 문서 버전을 놓고
 * 경합한다. 각 저장 메서드가 <b>자기 필드만</b> 건드리는 것이 재시도가 남의 입력을 되돌리지 않는
 * 유일한 근거이므로, 여기가 깨지면 실험실 입력이 조용히 사라진다.
 */
class AnalysisResultTest {

	private static AnalysisResult filled() {
		return new AnalysisResult(
			new BigDecimal("12.5"), "ppm", "자외선형광법", "분석기A",
			LocalTime.of(9, 30), LocalTime.of(11, 0));
	}

	@Nested
	@DisplayName("applyAnalysisResult — 실험·분석 탭")
	class ApplyAnalysisResult {

		@Test
		void 채취시각은_건드리지_않는다() {
			AnalysisResult updated = filled()
				.applyAnalysisResult(new BigDecimal("20"), "mg/S㎥", "중량법", "분석기B");

			assertThat(updated.analysisValue()).isEqualByComparingTo("20");
			assertThat(updated.unit()).isEqualTo("mg/S㎥");
			assertThat(updated.samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
			assertThat(updated.samplingEndedAt()).isEqualTo(LocalTime.of(11, 0));
		}

		@Test
		void 빈_값은_지운다() {
			AnalysisResult updated = filled().applyAnalysisResult(null, null, null, null);

			assertThat(updated.analysisValue()).isNull();
			assertThat(updated.unit()).isNull();
			assertThat(updated.analysisMethod()).isNull();
			assertThat(updated.analysisEquipment()).isNull();
			// 지우는 것은 자기 필드까지다 — 상대 탭의 값은 남는다.
			assertThat(updated.samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
		}
	}

	@Nested
	@DisplayName("applySamplingTime — 성적서 탭")
	class ApplySamplingTime {

		@Test
		void 실험실_입력값은_건드리지_않는다() {
			AnalysisResult updated = filled().applySamplingTime(LocalTime.of(13, 0), LocalTime.of(14, 0));

			assertThat(updated.samplingStartedAt()).isEqualTo(LocalTime.of(13, 0));
			assertThat(updated.samplingEndedAt()).isEqualTo(LocalTime.of(14, 0));
			assertThat(updated.analysisValue()).isEqualByComparingTo("12.5");
			assertThat(updated.unit()).isEqualTo("ppm");
			assertThat(updated.analysisMethod()).isEqualTo("자외선형광법");
			assertThat(updated.analysisEquipment()).isEqualTo("분석기A");
		}

		@Test
		void 빈_시각은_지운다() {
			AnalysisResult updated = filled().applySamplingTime(null, null);

			assertThat(updated.samplingStartedAt()).isNull();
			assertThat(updated.samplingEndedAt()).isNull();
			assertThat(updated.analysisValue()).isEqualByComparingTo("12.5");
		}

		@Test
		void 시작이_종료보다_늦어도_거부하지_않는다() {
			AnalysisResult updated = AnalysisResult.empty()
				.applySamplingTime(LocalTime.of(23, 30), LocalTime.of(1, 0));

			assertThat(updated.samplingStartedAt()).isEqualTo(LocalTime.of(23, 30));
			assertThat(updated.samplingEndedAt()).isEqualTo(LocalTime.of(1, 0));
		}
	}

	@Nested
	@DisplayName("isEmpty")
	class IsEmpty {

		@Test
		void 여섯_필드가_모두_비어야_빈_결과다() {
			assertThat(AnalysisResult.empty().isEmpty()).isTrue();
			assertThat(filled().isEmpty()).isFalse();
			assertThat(AnalysisResult.empty().applySamplingTime(LocalTime.of(9, 0), null).isEmpty()).isFalse();
		}
	}
}
