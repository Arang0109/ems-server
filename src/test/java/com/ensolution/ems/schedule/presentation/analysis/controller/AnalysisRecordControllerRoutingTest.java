package com.ensolution.ems.schedule.presentation.analysis.controller;

import com.ensolution.ems.schedule.application.service.AnalysisRecordService;
import com.ensolution.ems.schedule.presentation.analysis.mapper.AnalysisRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 경로 매칭 검증. {@code PUT /analyses/sampling-times}(리터럴)와 {@code PUT /analyses/{analysisId}}(변수)는
 * <b>같은 HTTP 메서드에서 겹치므로</b> 어느 핸들러가 잡히는지가 우연에 맡겨지면 안 된다.
 *
 * <p>Spring의 {@code PathPattern} 특이성 규칙상 리터럴이 먼저 매칭되지만, 컨트롤러 선언 순서를 바꾸거나
 * 경로 이름을 고칠 때 조용히 깨질 수 있는 지점이라 회귀 테스트로 고정해 둔다.
 * 라우팅만 보므로 Spring 컨텍스트·시큐리티 없이 standalone MockMvc로 띄운다.
 */
class AnalysisRecordControllerRoutingTest {

	private AnalysisRecordService service;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		service = mock(AnalysisRecordService.class);
		AnalysisRecordMapper mapper = mock(AnalysisRecordMapper.class);
		when(mapper.toResponses(any())).thenReturn(List.of());
		when(mapper.toResponse(any())).thenReturn(null);

		mockMvc = MockMvcBuilders
			.standaloneSetup(new AnalysisRecordController(service, mapper))
			.build();
	}

	@Test
	@DisplayName("PUT /analyses/sampling-times 는 일괄 저장 핸들러로 간다 — /{analysisId} 가 가로채지 않는다")
	void samplingTimesPathWinsOverAnalysisId() throws Exception {
		mockMvc.perform(put("/api/schedules/1/analyses/sampling-times")
				.contentType(APPLICATION_JSON)
				.content("{\"items\":[]}"))
			.andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200));

		verify(service).saveSamplingTimes(anyLong(), any(), any());
		verify(service, never()).updateAnalysis(anyLong(), any(), anyString(), any());
	}

	@Test
	@DisplayName("PUT /analyses/results 는 분석 결과 일괄 저장 핸들러로 간다")
	void resultsPathWinsOverAnalysisId() throws Exception {
		mockMvc.perform(put("/api/schedules/1/analyses/results")
				.contentType(APPLICATION_JSON)
				.content("{\"items\":[]}"))
			.andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200));

		verify(service).saveAnalysisResults(anyLong(), any(), any());
		verify(service, never()).updateAnalysis(anyLong(), any(), anyString(), any());
	}

	@Test
	@DisplayName("PUT /analyses/{analysisId} 는 여전히 단건 수정 핸들러로 간다")
	void analysisIdPathStillRoutesToUpdate() throws Exception {
		mockMvc.perform(put("/api/schedules/1/analyses/abc123")
				.contentType(APPLICATION_JSON)
				.content("{\"analysisValue\":10}"))
			.andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200));

		ArgumentCaptor<String> analysisId = ArgumentCaptor.forClass(String.class);
		verify(service).updateAnalysis(anyLong(), any(), analysisId.capture(), any());
		assertThat(analysisId.getValue()).isEqualTo("abc123");
		verify(service, never()).saveSamplingTimes(anyLong(), any(), any());
		verify(service, never()).saveAnalysisResults(anyLong(), any(), any());
	}
}
