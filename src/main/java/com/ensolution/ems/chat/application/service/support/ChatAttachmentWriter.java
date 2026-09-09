package com.ensolution.ems.chat.application.service.support;

import com.ensolution.ems.chat.application.command.AttachmentUpload;
import com.ensolution.ems.chat.application.command.ChatAttachmentFile;
import com.ensolution.ems.chat.domain.ChatAttachment;
import com.ensolution.ems.global.storage.FileStorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 첨부 실물의 쓰기·읽기 순서를 캡슐화합니다.
 *
 * <h2>왜 협력자인가</h2>
 * 파일 쓰기는 트랜잭션과 함께 롤백되지 않습니다. 그 사실을 서비스 본문에 흘려 두면 순서를
 * 잘못 바꾸기 쉬워서, "언제 쓰는가"를 이 클래스에 모읍니다. 서비스는 <b>메시지를 저장하기 직전에</b>
 * {@link #store}를 부르고, 그 뒤 Mongo insert 가 실패하면 고아 파일만 남습니다 —
 * 레코드가 가리키는 파일이 없는 것보다 덜 위험하다는 {@code storage}의 판단과 같습니다.
 *
 * <p>보관소 계약({@link FileStorageClient})은 {@code global/storage}의 공용 SPI 이며
 * {@code storage} 모듈과 공유합니다. 감싸기만 하는 포트를 이 모듈에 새로 두지 않습니다
 * (루트 규칙 3·10의 단순 위임 래퍼 금지).
 */
@Component
@RequiredArgsConstructor
public class ChatAttachmentWriter {

	private final FileStorageClient fileStorageClient;

	/**
	 * 키를 만들고 실물을 보관소에 쓴다. 크기·파일명 검증은 {@link ChatAttachment#register}가 한다.
	 *
	 * @return 메시지에 임베드할 첨부 메타
	 */
	public ChatAttachment store(Long tenantId, Long roomId, AttachmentUpload upload) {
		ChatAttachment attachment = ChatAttachment.register(
			tenantId, roomId, upload.originalFilename(), upload.contentType(), upload.size());

		fileStorageClient.store(attachment.getStorageKey(), upload.content());
		return attachment;
	}

	/** 원본 파일명을 함께 실어 준다 — 보관소의 키에는 그것이 들어 있지 않다. */
	public ChatAttachmentFile load(ChatAttachment attachment) {
		return new ChatAttachmentFile(
			attachment.getOriginalFilename(),
			attachment.getContentType(),
			fileStorageClient.load(attachment.getStorageKey())
		);
	}
}
