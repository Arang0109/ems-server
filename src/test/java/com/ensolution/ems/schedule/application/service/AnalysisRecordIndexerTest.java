package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.FakeAnalysisRecordRepository;
import com.ensolution.ems.schedule.domain.analysis.AnalysisRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실험분석정보 색인 검증.
 * 이 색인 규칙이 이행 기록과 성적서 내보내기 양쪽의 결합 근거이므로, 어긋난 데이터에서도
 * 예외로 경로를 막지 않고 조용히 걸러 내는 것이 핵심 계약이다.
 */
class AnalysisRecordIndexerTest {

	private static final Long TENANT_ID = 10L;
	private static final Long SCHEDULE_ID = 1L;

	private FakeAnalysisRecordRepository repository;
	private AnalysisRecordIndexer indexer;

	@BeforeEach
	void setUp() {
		repository = new FakeAnalysisRecordRepository();
		indexer = new AnalysisRecordIndexer(repository);
	}

	private AnalysisRecord analysis(Long pollutantId, String value) {
		return AnalysisRecord.builder()
			.tenantId(TENANT_ID)
			.scheduleId(SCHEDULE_ID)
			.pollutantId(pollutantId)
			.analysisValue(value == null ? null : new BigDecimal(value))
			.build();
	}

	@Test
	void 측정물질을_키로_색인한다() {
		repository.save(analysis(1L, "10"));
		repository.save(analysis(2L, "20"));

		Map<Long, AnalysisRecord> index = indexer.indexByPollutantId(SCHEDULE_ID, TENANT_ID);

		assertThat(index).containsOnlyKeys(1L, 2L);
		assertThat(index.get(1L).getAnalysisValue()).isEqualByComparingTo("10");
	}

	@Test
	void 측정물질_식별자가_없는_기록은_색인에서_제외된다() {
		// 어느 측정항목에도 붙일 수 없다. 예외 대신 제외해 완료·내보내기 경로를 막지 않는다
		repository.save(analysis(null, "10"));
		repository.save(analysis(2L, "20"));

		assertThat(indexer.indexByPollutantId(SCHEDULE_ID, TENANT_ID)).containsOnlyKeys(2L);
	}

	@Test
	void 같은_측정물질이_둘이면_나중_기록을_쓴다() {
		// 계획 내 유일성은 등록 시점 검사로만 지켜지므로, 어긋난 데이터가 경로를 막으면 안 된다
		repository.save(analysis(1L, "10"));
		repository.save(analysis(1L, "99"));

		Map<Long, AnalysisRecord> index = indexer.indexByPollutantId(SCHEDULE_ID, TENANT_ID);

		assertThat(index).containsOnlyKeys(1L);
		assertThat(index.get(1L).getAnalysisValue()).isEqualByComparingTo("99");
	}

	@Test
	void 분석_기록이_없으면_빈_색인이다() {
		assertThat(indexer.indexByPollutantId(SCHEDULE_ID, TENANT_ID)).isEmpty();
	}

	@Test
	void 다른_계획의_기록은_섞이지_않는다() {
		repository.save(analysis(1L, "10"));
		repository.save(analysis(2L, "20").toBuilder().scheduleId(999L).build());

		assertThat(indexer.indexByPollutantId(SCHEDULE_ID, TENANT_ID)).containsOnlyKeys(1L);
	}
}
