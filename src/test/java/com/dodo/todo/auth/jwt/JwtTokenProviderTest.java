package com.dodo.todo.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.dodo.todo.auth.config.JwtProperties;
import com.dodo.todo.auth.principal.MemberPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    @Test
    @DisplayName("access 토큰을 생성하고 회원 ID를 다시 읽을 수 있다")
    void generatesAndParsesValidAccessToken() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties(1800L, 604800L));
        MemberPrincipal principal = new MemberPrincipal(7L);

        String token = jwtTokenProvider.generateAccessToken(principal);

        assertThat(jwtTokenProvider.isValidAccessToken(token)).isTrue();
        assertThat(jwtTokenProvider.isValidRefreshToken(token)).isFalse();
        assertThat(jwtTokenProvider.getMemberId(token)).isEqualTo(7L);
        assertThat(jwtTokenProvider.getAccessTokenExpirationSeconds()).isEqualTo(1800L);
    }

    @Test
    @DisplayName("refresh 토큰을 생성하고 타입을 구분한다")
    void generatesValidRefreshToken() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties(1800L, 604800L));
        MemberPrincipal principal = new MemberPrincipal(7L);

        String token = jwtTokenProvider.generateRefreshToken(principal);

        assertThat(jwtTokenProvider.isValidRefreshToken(token)).isTrue();
        assertThat(jwtTokenProvider.isValidAccessToken(token)).isFalse();
        assertThat(jwtTokenProvider.getRefreshTokenExpirationSeconds()).isEqualTo(604800L);
    }

    @Test
    @DisplayName("같은 회원에게 연속 발급한 refresh 토큰은 jti로 인해 서로 달라진다")
    void generatesDistinctRefreshTokensForSameMember() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties(1800L, 604800L));
        MemberPrincipal principal = new MemberPrincipal(7L);

        String firstToken = jwtTokenProvider.generateRefreshToken(principal);
        String secondToken = jwtTokenProvider.generateRefreshToken(principal);

        assertThat(firstToken).isNotEqualTo(secondToken);
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 유효하지 않다")
    void rejectsMalformedToken() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties(1800L, 604800L));

        assertThat(jwtTokenProvider.isValidToken("not-a-token")).isFalse();
        assertThat(jwtTokenProvider.isValidAccessToken("not-a-token")).isFalse();
        assertThat(jwtTokenProvider.isValidRefreshToken("not-a-token")).isFalse();
    }

    @Test
    @DisplayName("만료된 access 토큰은 유효하지 않다")
    void rejectsExpiredAccessToken() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties(-1L, 604800L));
        MemberPrincipal principal = new MemberPrincipal(7L);

        String token = jwtTokenProvider.generateAccessToken(principal);

        assertThat(jwtTokenProvider.isValidAccessToken(token)).isFalse();
    }

    private JwtProperties jwtProperties(long accessExpirationSeconds, long refreshExpirationSeconds) {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-test-secret-key-1234");
        jwtProperties.setAccessTokenExpirationSeconds(accessExpirationSeconds);
        jwtProperties.setRefreshTokenExpirationSeconds(refreshExpirationSeconds);
        return jwtProperties;
    }
}
