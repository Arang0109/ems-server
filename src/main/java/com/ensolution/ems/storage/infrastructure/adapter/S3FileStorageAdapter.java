package com.ensolution.ems.storage.infrastructure.adapter;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.storage.application.port.out.FileStorageClient;
import com.ensolution.ems.storage.infrastructure.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3(및 S3 호환 스토리지)에 파일을 보관하는 {@link FileStorageClient} 구현체.
 * <p>
 * 실패 처리 규약은 {@link LocalFileStorageAdapter}와 같다 — 쓰기·읽기 실패는 예외를 던지고, 삭제 실패는
 * 로그만 남기고 삼킨다. 메타를 이미 지운 뒤에 파일을 지우므로 고아 오브젝트가 남는 편이
 * 레코드가 가리키는 파일이 없는 것보다 덜 위험하기 때문이다.
 * <p>
 * 로컬 구현이 하는 경로 이탈 검사는 두지 않는다. 오브젝트 키에는 상위로 벗어난다는 개념이 없고,
 * {@code storageKey}는 도메인이 UUID로 만들어 사용자 입력이 섞이지 않는다.
 * <p>
 * 빈 등록은 {@code S3Config}가 담당하며, {@code ems.storage.provider=S3}일 때만 등록된다.
 */
@Slf4j
@RequiredArgsConstructor
public class S3FileStorageAdapter implements FileStorageClient {

	private final S3Client s3Client;
	private final StorageProperties properties;

	@Override
	public void store(String storageKey, byte[] content) {
		String objectKey = objectKey(storageKey);
		try {
			s3Client.putObject(
				PutObjectRequest.builder().bucket(bucket()).key(objectKey).build(),
				RequestBody.fromBytes(content)
			);
		} catch (SdkException e) {
			log.error("S3 파일 저장에 실패했습니다. objectKey={}", objectKey, e);
			throw new CustomException(ErrorCode.STORAGE_WRITE_FAILED);
		}
	}

	@Override
	public byte[] load(String storageKey) {
		String objectKey = objectKey(storageKey);
		try {
			return s3Client.getObjectAsBytes(
				GetObjectRequest.builder().bucket(bucket()).key(objectKey).build()
			).asByteArray();
		} catch (NoSuchKeyException e) {
			log.error("메타는 있으나 S3에 실물 파일이 없습니다. objectKey={}", objectKey);
			throw new CustomException(ErrorCode.DOCUMENT_FILE_NOT_FOUND);
		} catch (SdkException e) {
			log.error("S3에서 파일을 읽지 못했습니다. objectKey={}", objectKey, e);
			throw new CustomException(ErrorCode.STORAGE_READ_FAILED);
		}
	}

	@Override
	public void delete(String storageKey) {
		String objectKey = objectKey(storageKey);
		try {
			s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket()).key(objectKey).build());
		} catch (SdkException e) {
			// 메타는 이미 지워졌다. 고아 오브젝트가 남더라도 삭제 자체를 실패시키지 않는다.
			log.warn("S3 파일 삭제에 실패했습니다. objectKey={}", objectKey, e);
		}
	}

	private String bucket() {
		return properties.s3().bucket();
	}

	/**
	 * 저장소 키 앞에 설정된 prefix를 붙인 오브젝트 키.
	 * prefix를 바꾸면 이전에 올린 파일을 찾지 못하므로 한 번 정하면 바꾸지 않는다.
	 */
	private String objectKey(String storageKey) {
		String prefix = properties.s3().keyPrefix();
		if (prefix == null || prefix.isBlank()) {
			return storageKey;
		}
		return prefix.replaceAll("/+$", "") + "/" + storageKey;
	}
}
