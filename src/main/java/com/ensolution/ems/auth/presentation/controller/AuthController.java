package com.ensolution.ems.auth.presentation.controller;

import com.ensolution.ems.auth.application.service.AuthService;
import com.ensolution.ems.auth.application.command.SignInResult;
import com.ensolution.ems.auth.application.service.RefreshTokenService;
import com.ensolution.ems.auth.presentation.mapper.SignInRequestMapper;
import com.ensolution.ems.auth.presentation.request.SignInRequest;
import com.ensolution.ems.auth.presentation.response.SignInResponse;
import com.ensolution.ems.global.security.domain.AuthCookieProperties;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증/인가 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

	private final AuthService authService;
	private final RefreshTokenService refreshTokenService;
	private final AuthCookieProperties cookieProperties;
	private final SignInRequestMapper signInMapper;

	// 공개 회원가입은 제공하지 않는다. 회원 생성은 테넌트 관리자 전용 POST /api/admin/members가 담당하며,
	// 테넌트의 최초 관리자는 platform 모듈의 테넌트 발급(provisionTenant)이 함께 만든다.

	@Operation(summary = "로그인", description = "Refresh Token을 HttpOnly 쿠키로 설정합니다.")
	@PostMapping("/sign-in")
	public ResponseEntity<ApiResponse<SignInResponse>> signIn(
			@RequestBody SignInRequest request,
			HttpServletResponse httpResponse
	) {
		// 로그인 요청 처리
		SignInResult signInResult = authService.signIn(signInMapper.toCommand(request));
		// Refresh Token을 DB저장
		refreshTokenService.saveRefreshToken(
			signInResult.username(),
			signInResult.refreshToken(),
			signInResult.tokenValidity()
		);
		
		ResponseCookie refreshCookie = createRefreshTokenCookie(
			signInResult.refreshToken(),
			signInResult.tokenValidity());
		
		httpResponse.setHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
		
		return ResponseEntity.ok().body(ApiResponse.success(signInMapper.toResponse(signInResult)));
	}

	@Operation(
		summary = "Access Token 재발급",
		description = """
			Refresh Token 쿠키로 Access Token을 다시 발급합니다. 응답의 data가 새 Access Token입니다.
			쿠키가 없거나 만료·폐기되었으면 401을 반환하므로, 클라이언트는 이때 로그아웃 처리합니다.
			Refresh Token 쿠키 자체는 갱신하지 않습니다.
			"""
	)
	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<String>> refresh(
		@CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken
	) {
		String accessToken = refreshTokenService.reissueAccessToken(refreshToken);
		return ResponseEntity.ok().body(ApiResponse.success(accessToken));
	}
	
	@Operation(summary = "로그아웃", description = "Refresh Token 쿠키를 삭제하고 만료 처리합니다.")
	@PostMapping("/sign-out")
	public ResponseEntity<ApiResponse<Void>> signOut(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		HttpServletResponse httpResponse
	) {
		refreshTokenService.deleteRefreshToken(userDetails.getUsername());
		ResponseCookie expiredRefreshCookie = createRefreshTokenCookie(null, 0);
		
		httpResponse.setHeader(HttpHeaders.SET_COOKIE, expiredRefreshCookie.toString());
		return ResponseEntity.ok().body(ApiResponse.success());
	}
	
	/**
	 * secure·SameSite 는 배포 환경마다 다르므로 설정에서 읽습니다.
	 * HTTP로 접속하는 개발 서버에 secure 쿠키를 내려보내면 브라우저가 조용히 버려
	 * 재발급이 영영 실패합니다({@link AuthCookieProperties} 참고).
	 */
	private ResponseCookie createRefreshTokenCookie(String refreshToken, long maxAge) {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
			.httpOnly(true)
			.secure(cookieProperties.secure())
			.path("/")
			.sameSite(cookieProperties.sameSite())
			.maxAge(maxAge)
			.build();
	}
}
