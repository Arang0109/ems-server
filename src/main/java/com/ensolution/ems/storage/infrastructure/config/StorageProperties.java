package com.ensolution.ems.storage.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 문서 보관소 설정.
 * <p>
 * 어느 보관소를 쓸지는 {@code ems.storage.provider}가 정하지만, 그 값은 이 record에 바인딩하지 않는다.
 * 보관소 구현체의 {@code @ConditionalOnProperty}가 Environment에서 직접 읽어 빈 등록 시점에 판단한다.
 * 여기에는 <b>선택된 보관소가 실제로 쓰는 설정</b>만 담는다.
 *
 * @param localRoot 로컬 저장소 루트 디렉토리. 컨테이너에서는 볼륨 마운트 경로를 주입한다
 * @param s3        S3 보관소 설정
 */
@ConfigurationProperties(prefix = "ems.storage")
public record StorageProperties(
	String localRoot,
	S3 s3
) {

	/**
	 * 자격증명 항목이 없는 것은 의도다. AWS SDK 기본 자격증명 체인이 IAM Role(EC2 인스턴스 프로파일·
	 * ECS Task Role·EKS IRSA)을 찾으므로 키를 설정에 두지 않는다.
	 *
	 * @param bucket          버킷 이름. {@code provider=S3}인데 비어 있으면 기동이 실패한다
	 * @param region          버킷 리전
	 * @param keyPrefix       오브젝트 키 앞에 붙는 경로. <b>한 번 정하면 바꾸지 않는다</b> —
	 *                        바꾸면 이전에 올린 파일을 찾지 못한다
	 * @param endpoint        S3 호환 스토리지(MinIO 등)용 엔드포인트. 비어 있으면 AWS 기본 엔드포인트를 쓴다
	 * @param pathStyleAccess 경로 기반 접근 여부. 호환 스토리지는 보통 true가 필요하다
	 */
	public record S3(
		String bucket,
		String region,
		String keyPrefix,
		String endpoint,
		boolean pathStyleAccess
	) {
	}
}
