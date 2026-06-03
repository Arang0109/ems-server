package com.ensolution.ems.auth.presentation;

import com.ensolution.ems.auth.application.AuthService;
import com.ensolution.ems.auth.application.command.SignInResult;
import com.ensolution.ems.auth.application.port.RefreshTokenStore;
import com.ensolution.ems.auth.presentation.mapper.SignInRequestMapper;
import com.ensolution.ems.auth.presentation.mapper.SignUpRequestMapper;
import com.ensolution.ems.auth.presentation.request.SignInRequest;
import com.ensolution.ems.auth.presentation.request.SignUpRequest;
import com.ensolution.ems.auth.presentation.response.SignInResponse;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증/인가 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final RefreshTokenStore refreshTokenStore;
	private final SignUpRequestMapper signUpMapper;
	private final SignInRequestMapper signInMapper;
	
	@PostMapping("/sign-up")
	public void signup(
			@Valid @RequestBody SignUpRequest request
	) { authService.signUp(signUpMapper.toCommand(request)); }
	
	@PostMapping("/sign-in")
	public ResponseEntity<ApiResponse<SignInResponse>> signIn(
			@RequestBody SignInRequest request,
			HttpServletResponse httpResponse
	) {
		// 로그인 요청 처리
		SignInResult signInResult = authService.signIn(signInMapper.toCommand(request));
		// Refresh Token을 DB저장
		refreshTokenStore.save(
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
	
	@PostMapping("/sign-out")
	public ResponseEntity<ApiResponse<Void>> signOut(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		HttpServletResponse httpResponse
	) {
		refreshTokenStore.delete(userDetails.getUsername());
		ResponseCookie expiredRefreshCookie = createRefreshTokenCookie(null, 0);
		
		httpResponse.setHeader(HttpHeaders.SET_COOKIE, expiredRefreshCookie.toString());
		return ResponseEntity.ok().body(ApiResponse.success());
	}
	
	private ResponseCookie createRefreshTokenCookie(String refreshToken, long maxAge) {
		return ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true)
			.secure(true)
			.path("/")
			.sameSite("None")
			.maxAge(maxAge)
			.build();
	}
}
