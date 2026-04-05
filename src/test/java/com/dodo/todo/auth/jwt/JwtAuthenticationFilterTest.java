package com.dodo.todo.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dodo.todo.auth.principal.MemberPrincipal;
import com.dodo.todo.auth.service.CustomUserDetailsService;
import com.dodo.todo.common.exception.ApiException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 access 토큰이면 SecurityContext에 인증 정보를 저장한다")
    void setsAuthenticationForValidToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService);
        MockHttpServletRequest request = requestWithBearerToken("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MemberPrincipal principal = new MemberPrincipal(3L, "user@example.com", "encoded", "user");

        when(jwtTokenProvider.isValidAccessToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getMemberId("valid-token")).thenReturn(3L);
        when(customUserDetailsService.loadUserById(3L)).thenReturn(principal);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(principal);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("잘못된 토큰이면 인증 정보를 설정하지 않는다")
    void skipsAuthenticationForMalformedToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService);
        MockHttpServletRequest request = requestWithBearerToken("bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.isValidAccessToken("bad-token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(customUserDetailsService, never()).loadUserById(anyLong());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰의 회원이 더 이상 없으면 인증 정보를 비운다")
    void clearsAuthenticationForStaleTokenMember() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService);
        MockHttpServletRequest request = requestWithBearerToken("stale-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.isValidAccessToken("stale-token")).thenReturn(true);
        when(jwtTokenProvider.getMemberId("stale-token")).thenReturn(99L);
        when(customUserDetailsService.loadUserById(99L))
                .thenThrow(new ApiException("MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND, "Member not found"));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    private MockHttpServletRequest requestWithBearerToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }
}
