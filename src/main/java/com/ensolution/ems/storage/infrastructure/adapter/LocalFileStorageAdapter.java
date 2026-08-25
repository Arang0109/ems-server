package com.ensolution.ems.storage.infrastructure.adapter;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.storage.application.port.out.FileStorageClient;
import com.ensolution.ems.storage.domain.StorageProvider;
import com.ensolution.ems.storage.infrastructure.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/** 로컬 디스크에 파일을 보관하는 {@link FileStorageClient} 구현체. */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalFileStorageAdapter implements FileStorageClient {

	private final StorageProperties properties;

	@Override
	public void store(String storageKey, byte[] content) {
		Path target = resolve(storageKey);
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, content);
		} catch (IOException e) {
			log.error("파일 저장에 실패했습니다. storageKey={}", storageKey, e);
			throw new CustomException(ErrorCode.STORAGE_WRITE_FAILED);
		}
	}

	@Override
	public byte[] load(String storageKey) {
		Path target = resolve(storageKey);
		try {
			return Files.readAllBytes(target);
		} catch (NoSuchFileException e) {
			log.error("메타는 있으나 실물 파일이 없습니다. storageKey={}", storageKey);
			throw new CustomException(ErrorCode.DOCUMENT_FILE_NOT_FOUND);
		} catch (IOException e) {
			log.error("파일을 읽지 못했습니다. storageKey={}", storageKey, e);
			throw new CustomException(ErrorCode.STORAGE_READ_FAILED);
		}
	}

	@Override
	public void delete(String storageKey) {
		try {
			Files.deleteIfExists(resolve(storageKey));
		} catch (IOException e) {
			// 메타는 이미 지워졌다. 고아 파일이 남더라도 삭제 자체를 실패시키지 않는다.
			log.warn("파일 삭제에 실패했습니다. storageKey={}", storageKey, e);
		}
	}

	@Override
	public StorageProvider provider() {
		return StorageProvider.LOCAL;
	}

	/** 저장소 루트를 벗어나는 키를 차단한다. */
	private Path resolve(String storageKey) {
		Path root = Path.of(properties.localRoot()).toAbsolutePath().normalize();
		Path target = root.resolve(storageKey).normalize();
		if (!target.startsWith(root)) {
			log.warn("저장소 루트를 벗어나는 키가 들어왔습니다. storageKey={}", storageKey);
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		return target;
	}
}
