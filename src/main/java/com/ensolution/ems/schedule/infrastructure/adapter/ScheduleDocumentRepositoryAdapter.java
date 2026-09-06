package com.ensolution.ems.schedule.infrastructure.adapter;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import com.ensolution.ems.schedule.infrastructure.document.ScheduleDocument;
import com.ensolution.ems.schedule.infrastructure.mapper.ScheduleDocumentMapper;
import com.ensolution.ems.schedule.infrastructure.repository.ScheduleDocumentMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ScheduleDocumentRepositoryAdapter implements ScheduleDocumentRepository {

	private final ScheduleDocumentMongoRepository scheduleDocumentMongoRepository;
	private final ScheduleDocumentMapper mapper;

	/**
	 * 도메인 스냅샷은 {@code createdAt}을 들고 다니지 않는다 — 문서의 저장 메타이지 측정 사실이 아니기
	 * 때문이다. 그런데 {@code save()}는 문서 전체를 치환하고 {@code @CreatedDate}는 신규 문서
	 * (version == null)에만 값을 채우므로, 그대로 두면 저장할 때마다 생성 시각이 null로 지워진다.
	 * 저장 직전에 기존 값을 읽어 되돌린다. 신규 문서면 읽히는 값이 없어 null이 들어가고,
	 * 그때는 auditing이 {@code @CreatedDate}를 채운다.
	 */
	@Override
	public ScheduleSnapshot save(ScheduleSnapshot snapshot) {
		ScheduleDocument document = mapper.toDocument(snapshot).toBuilder()
			.createdAt(existingCreatedAt(snapshot.id()))
			.build();
		return mapper.toDomain(scheduleDocumentMongoRepository.save(document));
	}

	private LocalDateTime existingCreatedAt(String id) {
		if (id == null) return null;
		return scheduleDocumentMongoRepository.findCreatedAtById(id)
			.map(ScheduleDocumentMongoRepository.CreatedAtView::getCreatedAt)
			.orElse(null);
	}

	@Override
	public ScheduleSnapshot findByScheduleId(Long scheduleId, Long tenantId) {
		return scheduleDocumentMongoRepository.findByScheduleIdAndTenantId(scheduleId, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_DOCUMENT_NOT_FOUND));
	}

	@Override
	public List<ScheduleSnapshot> findAll(Long tenantId) {
		return mapper.toDomains(scheduleDocumentMongoRepository.findByTenantId(tenantId));
	}

	@Override
	public void deleteByScheduleId(Long scheduleId, Long tenantId) {
		scheduleDocumentMongoRepository.deleteByScheduleIdAndTenantId(scheduleId, tenantId);
	}
}
