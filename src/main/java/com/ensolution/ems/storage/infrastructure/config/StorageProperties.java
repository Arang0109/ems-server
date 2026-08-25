package com.ensolution.ems.storage.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param localRoot 로컬 저장소 루트 디렉토리. 컨테이너에서는 볼륨 마운트 경로를 주입한다.
 */
@ConfigurationProperties(prefix = "ems.storage")
public record StorageProperties(
	String localRoot
) {
}
