package com.dodo.todo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.dodo.todo.auth.config.JwtProperties;
import com.dodo.todo.auth.dto.LoginRequest;
import com.dodo.todo.auth.dto.LoginResponse;
import com.dodo.todo.auth.dto.MemberResponse;
import com.dodo.todo.auth.dto.SignupRequest;
import com.dodo.todo.auth.jwt.JwtTokenProvider;
import com.dodo.todo.auth.principal.MemberPrincipal;
import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.domain.MemberRepository;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtTokenProvider = new JwtTokenProvider(jwtProperties(604800L));
        authService = new AuthService(memberRepository, passwordEncoder, authenticationManager, jwtTokenProvider);
    }

    @Test
    @DisplayName("회원가입 시 비밀번호를 암호화해 저장한다")
    void signupCreatesMemberWithEncodedPassword() {
        SignupRequest request = new SignupRequest("user@example.com", "password123", "user");
        AtomicReference<Member> savedMemberRef = new AtomicReference<>();

        when(memberRepository.existsByEmail(request.email())).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            savedMemberRef.set(member);
            return member;
        });

        MemberResponse response = authService.signup(request);
        Member savedMember = savedMemberRef.get();

        assertThat(savedMember).isNotNull();
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.nickname()).isEqualTo("user");
        assertThat(response.id()).isNull();
        assertThat(savedMember.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedMember.getPassword())).isTrue();
    }

    @Test
    @DisplayName("회원가입 시 중복 이메일이면 예외가 발생한다")
    void signupRejectsDuplicateEmail() {
        SignupRequest request = new SignupRequest("dup@example.com", "password123", "dup");
        when(memberRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Email already exists");
    }

    @Test
    @DisplayName("로그인 시 유효한 JWT와 회원 정보를 반환한다")
    void loginReturnsTokenAndMember() {
        LoginRequest request = new LoginRequest("login@example.com", "password123");
        MemberPrincipal principal = new MemberPrincipal(1L, "login@example.com", "encoded", "login-user");
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        LoginResponse response = authService.login(request);

        assertThat(jwtTokenProvider.isValidToken(response.token().accessToken())).isTrue();
        assertThat(jwtTokenProvider.getMemberId(response.token().accessToken())).isEqualTo(1L);
        assertThat(response.token().tokenType()).isEqualTo("Bearer");
        assertThat(response.token().expiresIn()).isEqualTo(604800L);
        assertThat(response.member().email()).isEqualTo("login@example.com");
    }

    @Test
    @DisplayName("로그인 시 자격 증명이 틀리면 예외가 발생한다")
    void loginRejectsInvalidCredentials() {
        LoginRequest request = new LoginRequest("login@example.com", "wrong-password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("현재 회원 조회 시 회원 정보를 반환한다")
    void getCurrentMemberReturnsMemberResponse() {
        Member member = Mockito.mock(Member.class);
        Mockito.when(member.getId()).thenReturn(10L);
        Mockito.when(member.getEmail()).thenReturn("current@example.com");
        Mockito.when(member.getNickname()).thenReturn("current-user");
        when(memberRepository.findById(10L)).thenReturn(Optional.of(member));

        MemberResponse response = authService.getCurrentMember(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.email()).isEqualTo("current@example.com");
        assertThat(response.nickname()).isEqualTo("current-user");
    }

    @Test
    @DisplayName("현재 회원 조회 시 회원이 없으면 예외가 발생한다")
    void getCurrentMemberRejectsMissingMember() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentMember(99L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Member not found");
    }

    private JwtProperties jwtProperties(long expirationSeconds) {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-test-secret-key-1234");
        jwtProperties.setAccessTokenExpirationSeconds(expirationSeconds);
        return jwtProperties;
    }
}