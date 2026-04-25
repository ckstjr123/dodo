package com.dodo.todo.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.dodo.todo.auth.config.JwtProperties;
import com.dodo.todo.auth.principal.MemberPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    @Test
    @DisplayName("access token을 생성하고 회원 ID를 파싱한다")
    void generatesAndParsesValidAccessToken() {
        long accessExpirationSeconds = 1800L;
        long refreshExpirationSeconds = 604800L;
        long memberId = 7L;
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties(accessExpirationSeconds, refreshExpirationSeconds));
        MemberPrincipal principal = new MemberPrincipal(memberId);

        String token = jwtTokenProvider.generateAccessToken(principal);

        assertThat(jwtTokenProvider.isValidAccessToken(token)).isTrue();
        assertThat(jwtTokenProvider.isValidRefreshToken(token)).isFalse();
        assertThat(jwtTokenProvider.getMemberId(token)).isEqualTo(memberId);
        assertThat(jwtTokenProvider.getAccessTokenExpirationSeconds()).isEqualTo(accessExpirationSeconds);
    }

    @Test
    @DisplayName("refresh token을 생성한다")
    void generatesValidRefreshToken() {
        long accessExpirationSeconds = 1800L;
        long refreshExpirationSeconds = 604800L;
        long memberId = 7L;
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties(accessExpirationSeconds, refreshExpirationSeconds));
        MemberPrincipal principal = new MemberPrincipal(memberId);

        String token = jwtTokenProvider.generateRefreshToken(principal);

        assertThat(jwtTokenProvider.isValidRefreshToken(token)).isTrue();
        assertThat(jwtTokenProvider.isValidAccessToken(token)).isFalse();
        assertThat(jwtTokenProvider.getRefreshTokenExpirationSeconds()).isEqualTo(refreshExpirationSeconds);
    }

    @Test
    @DisplayName("같은 회원이라도 refresh token은 매번 다르게 생성한다")
    void generatesDistinctRefreshTokensForSameMember() {
        long accessExpirationSeconds = 1800L;
        long refreshExpirationSeconds = 604800L;
        long memberId = 7L;
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties(accessExpirationSeconds, refreshExpirationSeconds));
        MemberPrincipal principal = new MemberPrincipal(memberId);

        String firstToken = jwtTokenProvider.generateRefreshToken(principal);
        String secondToken = jwtTokenProvider.generateRefreshToken(principal);

        assertThat(firstToken).isNotEqualTo(secondToken);
    }

    @Test
    @DisplayName("형식이 잘못된 token은 거부한다")
    void rejectsMalformedToken() {
        long accessExpirationSeconds = 1800L;
        long refreshExpirationSeconds = 604800L;
        String malformedToken = "not-a-token";
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties(accessExpirationSeconds, refreshExpirationSeconds));

        assertThat(jwtTokenProvider.isValidToken(malformedToken)).isFalse();
        assertThat(jwtTokenProvider.isValidAccessToken(malformedToken)).isFalse();
        assertThat(jwtTokenProvider.isValidRefreshToken(malformedToken)).isFalse();
    }

    @Test
    @DisplayName("만료된 access token은 거부한다")
    void rejectsExpiredAccessToken() {
        long expiredAccessExpirationSeconds = -1L;
        long refreshExpirationSeconds = 604800L;
        long memberId = 7L;
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
                jwtProperties(expiredAccessExpirationSeconds, refreshExpirationSeconds)
        );
        MemberPrincipal principal = new MemberPrincipal(memberId);

        String token = jwtTokenProvider.generateAccessToken(principal);

        assertThat(jwtTokenProvider.isValidAccessToken(token)).isFalse();
    }

    private JwtProperties jwtProperties(long accessExpirationSeconds, long refreshExpirationSeconds) {
        String secret = "test-secret-key-test-secret-key-1234";
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(secret);
        jwtProperties.setAccessTokenExpirationSeconds(accessExpirationSeconds);
        jwtProperties.setRefreshTokenExpirationSeconds(refreshExpirationSeconds);
        return jwtProperties;
    }
}
