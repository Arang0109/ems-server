package com.ensolution.ems.schedule.presentation.controller;

import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.schedule.application.service.CustomFieldDefinitionService;
import com.ensolution.ems.schedule.application.service.ScheduleExportService;
import com.ensolution.ems.schedule.application.service.ScheduleService;
import com.ensolution.ems.schedule.application.service.ScheduleSheetService;
import com.ensolution.ems.schedule.application.service.ScheduleSnapshotService;
import com.ensolution.ems.schedule.presentation.custom_field.controller.CustomFieldDefinitionController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;

/**
 * {@code /api/schedules} 아래 리터럴 경로가 {@code /{scheduleId}} 패턴보다 먼저 매칭되는지를 고정한다.
 *
 * <p>{@code custom-fields}(정의 관리)와 {@code sampling-records/template-check}(템플릿 검사)는 측정계획 id 자리에 리터럴이
 * 오는 경로다. Spring이 더 구체적인 패턴을 고르므로 지금은 문제없지만, 누군가 {@code /{scheduleId}/{anything}} 같은
 * 광범위한 패턴을 추가하면 조용히 가로채인다 — 그 회귀를 여기서 잡는다.
 *
 * <p>라우팅만 본다. 서비스는 의존성 없이 조립되어 호출되면 예외가 나고, 그 예외는 빈 {@code ModelAndView}로 삼킨다.
 * {@code @PreAuthorize}는 standaloneSetup에서 평가되지 않으므로 권한은 검증하지 않는다.
 */
class ScheduleRoutingTest {

	private static final CustomUserDetails PRINCIPAL =
		new CustomUserDetails(1L, 1L, "tenant", "user", "pw", "이름", List.of());

	/** {@code @AuthenticationPrincipal} 자리에 고정 principal을 넣는다 — 보안 컨텍스트 없이 라우팅만 보기 위함. */
	private static final HandlerMethodArgumentResolver PRINCIPAL_RESOLVER = new HandlerMethodArgumentResolver() {
		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return CustomUserDetails.class.isAssignableFrom(parameter.getParameterType());
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
		                              NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
			return PRINCIPAL;
		}
	};

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		ScheduleController scheduleController = new ScheduleController(
			new ScheduleService(null, null, null, null, null, null, null),
			new ScheduleSnapshotService(null, null, null, null, null, null, null),
			new ScheduleSheetService(null, null, null, null, null, null, null),
			null);
		CustomFieldDefinitionController customFieldController =
			new CustomFieldDefinitionController(new CustomFieldDefinitionService(null, null), null);
		ScheduleExportController exportController =
			new ScheduleExportController(new ScheduleExportService(null, null, null, null, null), null);

		mockMvc = MockMvcBuilders.standaloneSetup(scheduleController, customFieldController, exportController)
			.setCustomArgumentResolvers(PRINCIPAL_RESOLVER)
			// 핸들러 안에서 나는 예외(의존성 없는 서비스)는 라우팅 판정과 무관하므로 삼킨다.
			.setHandlerExceptionResolvers((request, response, handler, ex) -> new ModelAndView())
			.build();
	}

	@Test
	void custom_fields_목록은_정의_컨트롤러로_간다() throws Exception {
		mockMvc.perform(get("/api/schedules/custom-fields"))
			.andExpect(handler().handlerType(CustomFieldDefinitionController.class))
			.andExpect(handler().methodName("getDefinitionList"));
	}

	@Test
	void custom_fields_수정_삭제는_정의_컨트롤러로_간다() throws Exception {
		mockMvc.perform(put("/api/schedules/custom-fields/1")
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(handler().handlerType(CustomFieldDefinitionController.class))
			.andExpect(handler().methodName("updateDefinition"));

		mockMvc.perform(delete("/api/schedules/custom-fields/1"))
			.andExpect(handler().handlerType(CustomFieldDefinitionController.class))
			.andExpect(handler().methodName("deleteDefinition"));
	}

	@Test
	void 회차_custom_fields_저장은_측정계획_컨트롤러로_간다() throws Exception {
		mockMvc.perform(put("/api/schedules/1/custom-fields")
				.contentType(MediaType.APPLICATION_JSON).content("{\"values\":{}}"))
			.andExpect(handler().handlerType(ScheduleController.class))
			.andExpect(handler().methodName("saveCustomFields"));
	}

	@Test
	void template_check_는_내보내기_컨트롤러로_간다() throws Exception {
		mockMvc.perform(multipart("/api/schedules/sampling-records/template-check")
				.file(new MockMultipartFile("template", "t.xlsx", null, new byte[0])))
			.andExpect(handler().handlerType(ScheduleExportController.class))
			.andExpect(handler().methodName("checkTemplate"));
	}

	@Test
	void 기존_리터럴_경로와_id_경로는_그대로다() throws Exception {
		mockMvc.perform(get("/api/schedules/canceled"))
			.andExpect(handler().methodName("getCanceledScheduleList"));

		mockMvc.perform(get("/api/schedules/1"))
			.andExpect(handler().methodName("getSchedule"));

		mockMvc.perform(put("/api/schedules/1/sheets")
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(handler().methodName("saveSheets"));

		mockMvc.perform(multipart("/api/schedules/1/sampling-records/export")
				.file(new MockMultipartFile("template", "t.xlsx", null, new byte[0])))
			.andExpect(handler().methodName("exportSamplingRecords"));
	}
}
