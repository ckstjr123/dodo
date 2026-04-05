package com.dodo.todo.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.dodo.todo.auth.config.JwtProperties;
import com.dodo.todo.auth.principal.MemberPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    @Test
    @DisplayName("유효한 토큰을 생성하고 회원 ID를 다시 읽을 수 있다")
    void generatesAndParsesValidToken() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties(3600L));
        MemberPrincipal principal = new MemberPrincipal(7L, "user@example.com", "encoded", "user");

        String token = jwtTokenProvider.generateAccessToken(principal);

        assertThat(jwtTokenProvider.isValidToken(token)).isTrue();
        assertThat(jwtTokenProvider.getMemberId(token)).isEqualTo(7L);
        assertThat(jwtTokenProvider.getAccessTokenExpirationSeconds()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 유효하지 않다")
    void rejectsMalformedToken() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties(3600L));

        assertThat(jwtTokenProvider.isValidToken("not-a-token")).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 유효하지 않다")
    void rejectsExpiredToken() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties(-1L));
        MemberPrincipal principal = new MemberPrincipal(7L, "user@example.com", "encoded", "user");

        String token = jwtTokenProvider.generateAccessToken(principal);

        assertThat(jwtTokenProvider.isValidToken(token)).isFalse();
    }

    private JwtProperties jwtProperties(long expirationSeconds) {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-test-secret-key-1234");
        jwtProperties.setAccessTokenExpirationSeconds(expirationSeconds);
        return jwtProperties;
    }

}
