package com.dodo.todo.auth.service;

import com.dodo.todo.auth.dto.RefreshTokenRequest;
import com.dodo.todo.auth.dto.SocialLoginRequest;
import com.dodo.todo.auth.dto.TokenResponse;
import com.dodo.todo.auth.jwt.JwtTokenProvider;
import com.dodo.todo.auth.principal.MemberPrincipal;
import com.dodo.todo.auth.social.client.OAuthClients;
import com.dodo.todo.auth.social.domain.OAuthUserInfo;
import com.dodo.todo.auth.social.domain.SocialProvider;
import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.repository.MemberRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final OAuthClients oAuthClients;

    /**
     * 소셜 로그인
     * 소셜 access token으로 사용자 정보를 검증하고 서비스 JWT를 발급한다.
     */
    public TokenResponse login(SocialLoginRequest request) {
        SocialProvider provider = SocialProvider.from(request.provider());
        OAuthUserInfo userInfo = oAuthClients.authenticate(provider, request.accessToken());
        validateOAuthUserInfo(userInfo);

        Member member = findOrCreateMember(userInfo.email());
        return issueTokenResponse(member);
    }

    /**
     * refresh token 재발급
     * 요청 토큰과 회원에 저장된 토큰이 일치하면 새 토큰 쌍으로 교체한다.
     */
    public TokenResponse refresh(RefreshTokenRequest request) {
        if (!jwtTokenProvider.isValidRefreshToken(request.refreshToken())) {
            throw invalidRefreshTokenException();
        }

        Long memberId = jwtTokenProvider.getMemberId(request.refreshToken());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(this::invalidRefreshTokenException);

        if (member.getRefreshToken() == null
                || !member.getRefreshToken().isValidRefreshToken(request.refreshToken())) {
            throw invalidRefreshTokenException();
        }

        return issueTokenResponse(member);
    }

    private TokenResponse issueTokenResponse(Member member) {
        MemberPrincipal principal = new MemberPrincipal(member.getId());
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        member.changeRefreshToken(
                refreshToken,
                LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds())
        );
        return new TokenResponse(accessToken, refreshToken, "Bearer");
    }

    private void validateOAuthUserInfo(OAuthUserInfo userInfo) {
        if (userInfo.providerUserId() == null || userInfo.providerUserId().isBlank()) {
            throw new BusinessException(
                    "SOCIAL_AUTHENTICATION_FAILED",
                    HttpStatus.UNAUTHORIZED.value(),
                    "Social account id is missing"
            );
        }

        if (userInfo.email() == null || userInfo.email().isBlank()) {
            throw new BusinessException(
                    "SOCIAL_AUTHENTICATION_FAILED",
                    HttpStatus.UNAUTHORIZED.value(),
                    "Social account email is missing"
            );
        }

        if (!userInfo.emailVerified()) {
            throw new BusinessException(
                    "SOCIAL_AUTHENTICATION_FAILED",
                    HttpStatus.UNAUTHORIZED.value(),
                    "Social account email is not verified"
            );
        }
    }

    private Member findOrCreateMember(String email) {
        return memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(Member.from(email)));
    }

    private BusinessException invalidRefreshTokenException() {
        return new BusinessException(
                "INVALID_REFRESH_TOKEN",
                HttpStatus.UNAUTHORIZED.value(),
                "Refresh token is invalid"
        );
    }
}
