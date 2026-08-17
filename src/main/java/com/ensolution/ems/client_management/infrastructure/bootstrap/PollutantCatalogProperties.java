package com.ensolution.ems.client_management.infrastructure.bootstrap;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 측정물질 가이드(카탈로그) 부트스트랩 설정. */
@Component
@ConfigurationProperties(prefix = "pollutant.catalog")
@Getter
@Setter
public class PollutantCatalogProperties {

	/** 시드 활성화 여부. 카탈로그가 비어 있으면 물질 선택 자체가 불가능하므로 기본값이 true다. */
	private boolean seedEnabled = true;

	private String resourceLocation = "classpath:catalog/pollutant-catalog.json";
}
