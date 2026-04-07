package com.dodo.todo.auth.jwt;

import com.dodo.todo.auth.principal.MemberPrincipal;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = resolveToken(request);

            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            authenticate(request, token);
        } catch (JwtException | IllegalArgumentException | AuthenticationException exception) {
            commenceAuthenticationFailure(request, response, exception);
            return;
        }

        filterChain.doFilter(request, response);
    }

    // Access Token이 유효할 때만 SecurityContext에 인증 정보를 저장한다.
    private void authenticate(HttpServletRequest request, String token) {
        if (!StringUtils.hasText(token) || !jwtTokenProvider.isValidAccessToken(token)) {
            SecurityContextHolder.clearContext();
            throw new BadCredentialsException("Invalid access token");
        }

        Long memberId = jwtTokenProvider.getMemberId(token);
        MemberPrincipal memberPrincipal = new MemberPrincipal(memberId);
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        memberPrincipal,
                        null,
                        memberPrincipal.getAuthorities()
                );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // 인증 실패는 Security의 AuthenticationEntryPoint에서 401 응답으로 처리한다.
    private void commenceAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            RuntimeException exception
    ) throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException("Invalid access token", exception)
        );
    }

    private String resolveToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}
