package com.ensolution.ems.global.security.config;

import java.util.List;

/**
 * 브라우저에서 이 서버에 붙을 수 있는 오리진 목록.
 * <p>
 * <b>REST(CORS)와 WebSocket 핸드셰이크가 같은 값을 봐야 한다.</b> 두 곳에 따로 적어 두면
 * 오리진을 하나 추가할 때 한쪽만 고치게 되고, 그러면 배포 후 WebSocket만 조용히 막힌다 —
 * CORS 거부와 달리 핸드셰이크 실패는 API 호출에 아무 흔적을 남기지 않아 원인을 찾기 어렵다.
 *
 * @see SecurityConfig#corsConfigurationSource()
 * @see com.ensolution.ems.global.websocket.WebSocketConfig
 */
public final class AllowedOrigins {

	public static final List<String> PATTERNS = List.of(
		"http://localhost:5173",
		"http://127.0.0.1:5173",
		"http://54.180.112.112:3000",
		"https://env-bridge.co.kr",
		"https://www.env-bridge.co.kr"
	);

	private AllowedOrigins() {
	}
}
