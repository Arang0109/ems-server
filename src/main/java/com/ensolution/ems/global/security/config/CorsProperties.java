package com.ensolution.ems.global.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 브라우저에서 이 서버에 붙을 수 있는 오리진 목록.
 *
 * <h2>왜 설정으로 빼는가</h2>
 * 오리진은 환경마다 달라지는 값인데 상수로 박혀 있어, 배포 호스트가 바뀔 때 코드를 고치고 다시
 * 빌드해야 했습니다. 실제로 배포 오리진이 목록에서 빠진 채로 있었고 <b>그 사실이 드러나지 않았습니다</b> —
 * REST 는 nginx 를 통한 동일 오리진이라 CORS 프리플라이트가 발생하지 않기 때문입니다.
 * WebSocket 핸드셰이크는 동일 오리진이어도 {@code Origin} 을 검사하므로 거기서야 막힙니다.
 *
 * <h2>REST 와 WebSocket 이 같은 값을 봅니다</h2>
 * 두 곳에 따로 적어 두면 오리진을 하나 추가할 때 한쪽만 고치게 되고, 그러면 배포 후
 * WebSocket 만 조용히 막힙니다 — CORS 거부와 달리 핸드셰이크 실패는 API 호출에 아무 흔적을
 * 남기지 않아 원인을 찾기 어렵습니다.
 *
 * <p><b>이름을 {@code patterns} 가 아니라 {@code allowedOrigins} 로 둡니다.</b> 한때 이 리스트는
 * {@code PATTERNS} 라는 이름으로 CORS 에는 {@code setAllowedOrigins}(정확값), WebSocket 에는
 * {@code setAllowedOriginPatterns}(패턴)로 쓰이고 있었습니다. 와일드카드를 하나 넣는 순간
 * CORS 쪽만 조용히 깨지는 구조였습니다.
 *
 * @param allowedOrigins 정확한 오리진들. 와일드카드를 넣으려면 CORS 쪽 설정도 함께 바꿔야 합니다
 * @see SecurityConfig#corsConfigurationSource()
 * @see com.ensolution.ems.global.websocket.WebSocketConfig
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
	List<String> allowedOrigins
) {
}
