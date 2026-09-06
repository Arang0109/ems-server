package com.ensolution.ems.platform.application.service;

import com.ensolution.ems.platform.application.mapper.TenantSummaryMapper;
import com.ensolution.ems.platform.application.port.in.TenantQueryUseCase;
import com.ensolution.ems.platform.application.port.in.TenantSummary;
import com.ensolution.ems.platform.application.port.out.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이 모듈이 밖으로 여는 계약({@link TenantQueryUseCase})의 구현.
 * <p>
 * 운영자 유스케이스({@code PlatformService})와 분리해 둔 이유는 <b>소비자가 다르고, 의존이 다르기</b>
 * 때문이다. {@code schedule}의 {@code ScheduleStatisticsService}가 같은 이유로 분리돼 있다.
 * <p>
 * <b>의존이 다르다는 점이 여기서는 특히 중요하다.</b> {@code PlatformService}는 테넌트 발급 시
 * 초기 관리자를 만들기 위해 auth의 {@code UserCommandUseCase}를 쓴다. 그 서비스가 이 계약까지 함께
 * 구현하면 다음 순환이 생긴다:
 * <pre>
 * AuthService → Authenticator → CustomUserDetailsService → TenantQueryUseCase
 *            → PlatformService → UserCommandUseCase → AuthService
 * </pre>
 * 조회 계약을 auth를 모르는 이 서비스가 맡으면 고리가 끊어진다.
 * <b>여기에 auth 의존을 들이지 말 것</b> — 들이는 순간 순환이 되살아난다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantQueryService implements TenantQueryUseCase {

	private final TenantRepository tenantRepository;
	private final TenantSummaryMapper tenantSummaryMapper;

	/**
	 * 타 모듈(schedule의 스냅샷 조립, global의 인증 주체 조립)이 쓰는 고객사 요약.
	 * 미존재 시 {@code TENANT_NOT_FOUND}.
	 */
	@Override
	public TenantSummary getTenantSummary(Long tenantId) {
		return tenantSummaryMapper.toSummary(tenantRepository.findById(tenantId));
	}
}
