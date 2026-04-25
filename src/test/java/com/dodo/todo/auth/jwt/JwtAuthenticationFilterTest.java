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
    @DisplayName("유효한 access token이면 회원 인증 정보를 저장한다")
    void setsAuthenticationForValidToken() throws Exception {
        String validToken = "valid-token";
        Long memberId = 3L;
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authenticationEntryPoint);
        MockHttpServletRequest request = requestWithBearerToken(validToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.isValidAccessToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getMemberId(validToken)).thenReturn(memberId);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isInstanceOfSatisfying(MemberPrincipal.class, principal -> assertThat(principal.getId()).isEqualTo(memberId));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("인증 헤더가 없으면 다음 보안 흐름으로 진행한다")
    void proceedsWithoutAuthenticationWhenAuthorizationHeaderIsMissing() throws Exception {
        String method = "GET";
        String path = "/api/v1/todos";
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 형식이 아니면 인증 없이 다음 보안 흐름으로 진행한다")
    void proceedsWithoutAuthenticationWhenAuthorizationHeaderIsNotBearer() throws Exception {
        String authorizationHeader = "Basic token";
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authenticationEntryPoint);
        MockHttpServletRequest request = requestWithAuthorization(authorizationHeader);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("이미 인증 정보가 있으면 token을 다시 검증하지 않는다")
    void skipsAuthenticationWhenSecurityContextAlreadyHasAuthentication() throws Exception {
        String validToken = "valid-token";
        Long memberId = 1L;
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authenticationEntryPoint);
        MockHttpServletRequest request = requestWithBearerToken(validToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = authenticatedMember(memberId);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("비어 있는 Bearer token이면 인증 실패 응답으로 끝낸다")
    void commencesAuthenticationFailureWhenBearerTokenIsBlank() throws Exception {
        String blankBearerToken = "Bearer ";
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authenticationEntryPoint);
        MockHttpServletRequest request = requestWithAuthorization(blankBearerToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authenticationEntryPoint).commence(any(), any(), any(BadCredentialsException.class));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 token이면 인증 실패 응답으로 끝낸다")
    void commencesAuthenticationFailureForInvalidToken() throws Exception {
        String invalidToken = "bad-token";
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authenticationEntryPoint);
        MockHttpServletRequest request = requestWithBearerToken(invalidToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.isValidAccessToken(invalidToken)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authenticationEntryPoint).commence(any(), any(), any(BadCredentialsException.class));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("token에서 회원 ID를 읽지 못하면 인증 실패 응답으로 끝낸다")
    void commencesAuthenticationFailureWhenMemberIdCannotBeRead() throws Exception {
        String staleToken = "stale-token";
        String invalidSubjectMessage = "invalid subject";
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authenticationEntryPoint);
        MockHttpServletRequest request = requestWithBearerToken(staleToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.isValidAccessToken(staleToken)).thenReturn(true);
        when(jwtTokenProvider.getMemberId(staleToken)).thenThrow(new IllegalArgumentException(invalidSubjectMessage));

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
        String bearerPrefix = "Bearer ";
        return requestWithAuthorization(bearerPrefix + token);
    }

    private MockHttpServletRequest requestWithAuthorization(String authorizationHeader) {
        String method = "GET";
        String path = "/api/v1/todos";
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
        return request;
    }
}
