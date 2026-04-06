package com.dodo.todo.auth.service;

import com.dodo.todo.auth.domain.RefreshToken;
import com.dodo.todo.auth.domain.RefreshTokenRepository;
import com.dodo.todo.auth.dto.MemberResponse;
import com.dodo.todo.auth.dto.RefreshTokenRequest;
import com.dodo.todo.auth.dto.SocialLoginRequest;
import com.dodo.todo.auth.dto.TokenResponse;
import com.dodo.todo.auth.jwt.JwtTokenProvider;
import com.dodo.todo.auth.principal.MemberPrincipal;
import com.dodo.todo.auth.social.client.OAuthClient;
import com.dodo.todo.auth.social.domain.OAuthUserInfo;
import com.dodo.todo.auth.social.domain.SocialProvider;
import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.domain.MemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_REFRESH_TOKEN_SESSIONS = 2;

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final List<OAuthClient> oAuthClients;

    @Transactional
    public TokenResponse login(SocialLoginRequest request) {
        SocialProvider provider = SocialProvider.from(request.provider());
        OAuthUserInfo userInfo = authenticate(provider, request.authorizationCode(), request.redirectUri());
        validateOAuthUserInfo(userInfo);

        Member member = findOrCreateMember(userInfo.email());
        return issueTokenResponse(new MemberPrincipal(member.getId(), member.getEmail()));
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        if (!jwtTokenProvider.isValidRefreshToken(request.refreshToken())) {
            throw new ApiException("INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
        }

        RefreshToken storedRefreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new ApiException("INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED, "Refresh token is invalid"));
        LocalDateTime now = LocalDateTime.now();

        if (storedRefreshToken.isExpired(now)) {
            refreshTokenRepository.delete(storedRefreshToken);
            throw new ApiException("INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
        }

        Long memberId = jwtTokenProvider.getMemberId(storedRefreshToken.getToken());
        MemberPrincipal principal = customUserDetailsService.loadUserById(memberId);

        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);
        LocalDateTime expiredAt = now.plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds());
        // 기존 토큰값을 조건으로 갱신해 동시 refresh 요청이 같은 세션을 덮어쓰지 못하게 막는다.
        int updatedCount = refreshTokenRepository.rotateToken(
                storedRefreshToken.getId(),
                request.refreshToken(),
                refreshToken,
                expiredAt,
                now
        );
        if (updatedCount == 0) {
            throw new ApiException("INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
        }
        cleanUpRefreshTokens(memberId);

        return new TokenResponse(accessToken, refreshToken, "Bearer");
    }

    @Transactional(readOnly = true)
    public MemberResponse getCurrentMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException("MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND, "Member not found"));

        return new MemberResponse(member.getId(), member.getEmail());
    }

    @Transactional
    public TokenResponse issueTokenResponse(MemberPrincipal principal) {
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);
        createRefreshToken(principal.getId(), refreshToken);

        return new TokenResponse(accessToken, refreshToken, "Bearer");
    }

    private OAuthUserInfo authenticate(SocialProvider provider, String authorizationCode, String redirectUri) {
        OAuthClient oAuthClient = oAuthClients.stream()
                .filter(client -> client.supports(provider))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        "UNSUPPORTED_SOCIAL_PROVIDER",
                        HttpStatus.BAD_REQUEST,
                        "Unsupported social provider"
                ));

        // provider별 구현체를 선택해도 AuthService의 로그인 흐름은 하나로 유지한다.
        return oAuthClient.authenticate(authorizationCode, redirectUri);
    }

    private void validateOAuthUserInfo(OAuthUserInfo userInfo) {
        if (userInfo.providerUserId() == null || userInfo.providerUserId().isBlank()) {
            throw new ApiException(
                    "SOCIAL_AUTHENTICATION_FAILED",
                    HttpStatus.UNAUTHORIZED,
                    "Social account id is missing"
            );
        }

        if (userInfo.email() == null || userInfo.email().isBlank()) {
            throw new ApiException(
                    "SOCIAL_AUTHENTICATION_FAILED",
                    HttpStatus.UNAUTHORIZED,
                    "Social account email is missing"
            );
        }

        if (!userInfo.emailVerified()) {
            throw new ApiException(
                    "SOCIAL_AUTHENTICATION_FAILED",
                    HttpStatus.UNAUTHORIZED,
                    "Social account email is not verified"
            );
        }
    }

    private Member findOrCreateMember(String email) {
        return memberRepository.findByEmail(email)
                .orElseGet(() -> createMember(email));
    }

    private Member createMember(String email) {
        try {
            return memberRepository.save(Member.builder()
                    .email(email)
                    .build());
        } catch (DataIntegrityViolationException exception) {
            // 동시 가입 경합이 발생하면 이미 저장된 회원을 다시 조회해 재사용한다.
            return memberRepository.findByEmail(email)
                    .orElseThrow(() -> exception);
        }
    }

    private void createRefreshToken(Long memberId, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .memberId(memberId)
                .token(token)
                .expiredAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds()))
                .build();
        refreshTokenRepository.save(refreshToken);
        cleanUpRefreshTokens(memberId);
    }

    private void cleanUpRefreshTokens(Long memberId) {
        // 최근에 사용된 세션 2개만 남기고, 그보다 오래된 리프레시 토큰은 제거한다.
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByMemberIdOrderByUpdatedAtDescIdDesc(memberId);
        if (refreshTokens.size() <= MAX_REFRESH_TOKEN_SESSIONS) {
            return;
        }

        refreshTokenRepository.deleteAll(refreshTokens.subList(MAX_REFRESH_TOKEN_SESSIONS, refreshTokens.size()));
    }
}
