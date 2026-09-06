package com.ensolution.ems.storage.infrastructure.config;

import com.ensolution.ems.storage.infrastructure.adapter.S3FileStorageAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

/**
 * S3 보관소 배선. {@code ems.storage.provider=S3}일 때만 등록된다.
 * <p>
 * 이 조건이 성립하면 {@code LocalFileStorageAdapter}는 뜨지 않는다. 한 환경에 보관소는 하나뿐이며,
 * 과거 파일이 어느 보관소에 있는지 따라가는 라우팅은 두지 않는다.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "ems.storage.provider", havingValue = "S3")
public class S3Config {

	/**
	 * 자격증명은 지정하지 않는다. AWS SDK 기본 체인이 IAM Role(EC2 인스턴스 프로파일·ECS Task Role·
	 * EKS IRSA)을 찾으므로 키를 설정이나 코드에 두지 않는다. MinIO 등으로 로컬 검증할 때만
	 * {@code AWS_ACCESS_KEY_ID}/{@code AWS_SECRET_ACCESS_KEY}를 환경변수로 주면 같은 체인이 집어 든다.
	 */
	@Bean
	public S3Client s3Client(StorageProperties properties) {
		StorageProperties.S3 s3 = properties.s3();

		// 버킷 없이 뜨면 첫 업로드에서 500이 난다. 그 전에 기동을 막는다.
		if (s3 == null || s3.bucket() == null || s3.bucket().isBlank()) {
			throw new IllegalStateException(
				"ems.storage.provider=S3 이지만 버킷(ems.storage.s3.bucket, 환경변수 S3_BUCKET)이 비어 있습니다."
			);
		}

		S3ClientBuilder builder = S3Client.builder().region(Region.of(s3.region()));

		// S3 호환 스토리지(MinIO 등)를 가리킬 때만 엔드포인트를 덮어쓴다.
		if (s3.endpoint() != null && !s3.endpoint().isBlank()) {
			builder.endpointOverride(URI.create(s3.endpoint()))
				.forcePathStyle(s3.pathStyleAccess());
			log.info("S3 엔드포인트를 재지정했습니다. endpoint={}", s3.endpoint());
		} else if (s3.pathStyleAccess()) {
			builder.forcePathStyle(true);
		}

		log.info("S3 문서 보관소를 사용합니다. bucket={}, region={}", s3.bucket(), s3.region());

		return builder.build();
	}

	@Bean
	public S3FileStorageAdapter s3FileStorageAdapter(S3Client s3Client, StorageProperties properties) {
		return new S3FileStorageAdapter(s3Client, properties);
	}
}
