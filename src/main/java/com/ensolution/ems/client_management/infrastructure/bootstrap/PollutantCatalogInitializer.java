package com.ensolution.ems.client_management.infrastructure.bootstrap;

import com.ensolution.ems.client_management.application.command.create.CreatePollutantCatalogCommand;
import com.ensolution.ems.client_management.application.port.out.PollutantCatalogRepository;
import com.ensolution.ems.client_management.application.service.PollutantCatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 고객사에게 지원하는 측정물질 가이드(카탈로그)를 확보하는 부트스트랩.
 *
 * <p>계층 규칙의 명시적 예외다 — infrastructure에 있으면서 아웃바운드 포트와 유스케이스를 직접 조합한다.
 * 런타임 유스케이스가 아니라 배포 시 1회성 설치 코드이므로 현 위치를 유지하며,
 * 예외 범위는 이 클래스에 한정한다(platform 모듈의 {@code PlatformAdminInitializer}와 같은 성격).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PollutantCatalogInitializer implements ApplicationRunner {

	private final PollutantCatalogProperties properties;
	private final PollutantCatalogService pollutantCatalogService;
	private final PollutantCatalogRepository pollutantCatalogRepository;
	private final ObjectMapper objectMapper;

	/**
	 * 시드 파일은 {@code classpath:} 하나만 읽으므로 컨테이너 빈을 주입받지 않는다.
	 * {@code ResourceLoader}는 {@code ApplicationContext}를 포함해 구현체가 여럿이라 주입 후보가 모호하다.
	 */
	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!properties.isSeedEnabled()) {
			log.info("[pollutant-catalog] 시드가 비활성화되어 있어 건너뜁니다.");
			return;
		}

		List<PollutantCatalogSeedItem> seedItems = loadSeedItems();
		if (seedItems.isEmpty()) return;

		seed(seedItems);
	}

	private List<PollutantCatalogSeedItem> loadSeedItems() {
		try (InputStream in = resourceLoader.getResource(properties.getResourceLocation()).getInputStream()) {
			return List.of(objectMapper.readValue(in, PollutantCatalogSeedItem[].class));
		} catch (IOException e) {
			log.error("[pollutant-catalog] 시드 파일을 읽지 못했습니다: {}", properties.getResourceLocation(), e);
			return List.of();
		}
	}

	/** 없는 code만 추가한다. 이미 있는 항목은 운영자가 수정했을 수 있으므로 덮어쓰지 않는다. */
	private void seed(List<PollutantCatalogSeedItem> seedItems) {
		int created = 0;
		for (PollutantCatalogSeedItem item : seedItems) {
			if (pollutantCatalogRepository.existsByFieldAndCode(item.field(), item.code())) continue;

			pollutantCatalogService.ensureCatalog(new CreatePollutantCatalogCommand(
				item.code(), item.field(), item.nameKr(),
				item.method(), item.phase(), item.sortOrder()
			));
			created++;
		}
		log.info("[pollutant-catalog] 시드 완료 — 생성 {}건 / 기존 {}건", created, seedItems.size() - created);
	}
}
