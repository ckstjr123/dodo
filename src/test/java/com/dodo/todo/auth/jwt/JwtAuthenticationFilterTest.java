package com.dodo.todo.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dodo.todo.auth.principal.MemberPrincipal;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 access token이면 토큰의 회원 ID로 인증 정보를 저장한다")
    void setsAuthenticationForValidToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authenticationEntryPoint);
        MockHttpServletRequest request = requestWithBearerToken("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.isValidAccessToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getMemberId("valid-token")).thenReturn(3L);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isInstanceOfSatisfying(MemberPrincipal.class, principal -> assertThat(principal.getId()).isEqualTo(3L));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("인증 토큰이 없으면 인증 정보 없이 다음 보안 흐름으로 넘긴다")
    void proceedsWithoutAuthenticationWhenAuthorizationHeaderIsMissing() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/todos");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 형식이 아니면 인증 정보 없이 다음 보안 흐름으로 넘긴다")
    void proceedsWithoutAuthenticationWhenAuthorizationHeaderIsNotBearer() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authenticationEntryPoint);
        MockHttpServletRequest request = requestWithAuthorization("Basic token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("이미 인증 정보가 있으면 토큰을 다시 검증하지 않는다")
    void skipsAuthenticationWhenSecurityContextAlreadyHasAuthentication() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authenticationEntryPoint);
        MockHttpServletRequest request = requestWithBearerToken("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = authenticatedMember(1L);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("비어 있는 Bearer token이면 인증 실패 응답으로 넘긴다")
    void commencesAuthenticationFailureWhenBearerTokenIsBlank() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authenticationEntryPoint);
        MockHttpServletRequest request = requestWithAuthorization("Bearer ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authenticationEntryPoint).commence(any(), any(), any(BadCredentialsException.class));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("만료됐거나 유효하지 않은 token이면 인증 실패 응답으로 넘긴다")
    void commencesAuthenticationFailureForInvalidToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authenticationEntryPoint);
        MockHttpServletRequest request = requestWithBearerToken("bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.isValidAccessToken("bad-token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authenticationEntryPoint).commence(any(), any(), any(BadCredentialsException.class));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("token에서 회원 ID를 읽을 수 없으면 인증 실패 응답으로 넘긴다")
    void commencesAuthenticationFailureWhenMemberIdCannotBeRead() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authenticationEntryPoint);
        MockHttpServletRequest request = requestWithBearerToken("stale-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.isValidAccessToken("stale-token")).thenReturn(true);
        when(jwtTokenProvider.getMemberId("stale-token")).thenThrow(new IllegalArgumentException("invalid subject"));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authenticationEntryPoint).commence(any(), any(), any(BadCredentialsException.class));
        verify(filterChain, never()).doFilter(request, response);
    }

    private Authentication authenticatedMember(Long memberId) {
        MemberPrincipal principal = new MemberPrincipal(memberId);
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    private MockHttpServletRequest requestWithBearerToken(String token) {
        return requestWithAuthorization("Bearer " + token);
    }

    private MockHttpServletRequest requestWithAuthorization(String authorizationHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/todos");
        request.addHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
        return request;
    }
}
