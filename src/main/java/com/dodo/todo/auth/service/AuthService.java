package com.dodo.todo.auth.service;

import com.dodo.todo.auth.dto.LoginRequest;
import com.dodo.todo.auth.dto.LoginResponse;
import com.dodo.todo.auth.dto.MemberResponse;
import com.dodo.todo.auth.dto.RefreshTokenRequest;
import com.dodo.todo.auth.dto.SignupRequest;
import com.dodo.todo.auth.dto.TokenResponse;
import com.dodo.todo.auth.jwt.JwtTokenProvider;
import com.dodo.todo.auth.principal.MemberPrincipal;
import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new ApiException("EMAIL_ALREADY_EXISTS", HttpStatus.CONFLICT, "Email already exists");
        }

        Member member = memberRepository.save(Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build());

        return toMemberResponse(member);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();

            return new LoginResponse(
                    createTokenResponse(principal),
                    new MemberResponse(principal.getId(), principal.getEmail(), principal.getNickname())
            );
        } catch (BadCredentialsException exception) {
            throw new ApiException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshTokenRequest request) {
        if (!jwtTokenProvider.isValidRefreshToken(request.refreshToken())) {
            throw new ApiException("INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
        }

        Long memberId = jwtTokenProvider.getMemberId(request.refreshToken());
        MemberPrincipal principal = customUserDetailsService.loadUserById(memberId);

        // 리프레시 토큰 재발급 시 access/refresh 토큰 쌍을 함께 갱신한다.
        return createTokenResponse(principal);
    }

    @Transactional(readOnly = true)
    public MemberResponse getCurrentMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException("MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND, "Member not found"));

        return toMemberResponse(member);
    }

    private MemberResponse toMemberResponse(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getNickname());
    }
    private TokenResponse createTokenResponse(MemberPrincipal principal) {
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                jwtTokenProvider.getRefreshTokenExpirationSeconds()
        );
    }
}
