package com.dodo.todo.auth.service;

import com.dodo.todo.auth.domain.RefreshToken;
import com.dodo.todo.auth.repository.RefreshTokenRepository;
import com.dodo.todo.auth.dto.MemberResponse;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
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
    private final OAuthClients oAuthClients;

    public TokenResponse login(SocialLoginRequest request) {
        SocialProvider provider = SocialProvider.from(request.provider());
        OAuthUserInfo userInfo = oAuthClients.authenticate(provider, request.authorizationCode(), request.redirectUri());
        validateOAuthUserInfo(userInfo);

        return completeLogin(userInfo);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        if (!jwtTokenProvider.isValidRefreshToken(request.refreshToken())) {
            throw new BusinessException(
                    "INVALID_REFRESH_TOKEN",
                    HttpStatus.UNAUTHORIZED.value(),
                    "Refresh token is invalid"
            );
        }

        RefreshToken storedRefreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException(
                        "INVALID_REFRESH_TOKEN",
                        HttpStatus.UNAUTHORIZED.value(),
                        "Refresh token is invalid"
                ));
        LocalDateTime now = LocalDateTime.now();

        if (storedRefreshToken.isExpired(now)) {
            refreshTokenRepository.delete(storedRefreshToken);
            throw new BusinessException(
                    "INVALID_REFRESH_TOKEN",
                    HttpStatus.UNAUTHORIZED.value(),
                    "Refresh token is invalid"
            );
        }

        Long memberId = storedRefreshToken.getMemberId();
        MemberPrincipal principal = new MemberPrincipal(memberId);

        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);
        storedRefreshToken.rotate(
                refreshToken,
                now.plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds())
        );
        cleanUpRefreshTokens(memberId);

        return new TokenResponse(accessToken, refreshToken, "Bearer");
    }

    @Transactional(readOnly = true)
    public MemberResponse getCurrentMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        "MEMBER_NOT_FOUND",
                        HttpStatus.NOT_FOUND.value(),
                        "Member not found"
                ));

        return new MemberResponse(member.getId(), member.getEmail());
    }

    @Transactional
    public TokenResponse issueTokenResponse(MemberPrincipal principal) {
        Member member = memberRepository.getReferenceById(principal.getId());
        return issueTokenResponse(member);
    }

    private TokenResponse issueTokenResponse(Member member) {
        MemberPrincipal principal = new MemberPrincipal(member.getId());
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);
        createRefreshToken(member, refreshToken);

        return new TokenResponse(accessToken, refreshToken, "Bearer");
    }

    @Transactional
    protected TokenResponse completeLogin(OAuthUserInfo userInfo) {
        Member member = findOrCreateMember(userInfo.email());
        return issueTokenResponse(member);
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
                .orElseGet(() -> memberRepository.save(Member.of(email)));
    }

    private void createRefreshToken(Member member, String token) {
        RefreshToken refreshToken = new RefreshToken(
                member,
                token,
                LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds())
        );
        refreshTokenRepository.save(refreshToken);
        cleanUpRefreshTokens(member.getId());
    }

    private void cleanUpRefreshTokens(Long memberId) {
        // 최근 사용 기준으로 최대 2개의 세션만 남긴다.
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByMember_IdOrderByUpdatedAtDescIdDesc(memberId);
        if (refreshTokens.size() <= MAX_REFRESH_TOKEN_SESSIONS) {
            return;
        }

        refreshTokenRepository.deleteAll(refreshTokens.subList(MAX_REFRESH_TOKEN_SESSIONS, refreshTokens.size()));
    }
}
