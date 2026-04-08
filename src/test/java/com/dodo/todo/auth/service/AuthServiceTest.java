package com.dodo.todo.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dodo.todo.auth.config.JwtProperties;
import com.dodo.todo.auth.domain.RefreshToken;
import com.dodo.todo.auth.domain.RefreshTokenRepository;
import com.dodo.todo.auth.dto.MemberResponse;
import com.dodo.todo.auth.dto.RefreshTokenRequest;
import com.dodo.todo.auth.dto.SocialLoginRequest;
import com.dodo.todo.auth.dto.TokenResponse;
import com.dodo.todo.auth.jwt.JwtTokenProvider;
import com.dodo.todo.auth.principal.MemberPrincipal;
import com.dodo.todo.auth.social.client.OAuthClients;
import com.dodo.todo.auth.social.domain.OAuthUserInfo;
import com.dodo.todo.auth.social.domain.SocialProvider;
import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.domain.MemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private OAuthClients oAuthClients;

    @Spy
    private JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties(1800L, 604800L));

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("소셜 로그인 시 기존 회원 이메일과 일치하면 기존 회원으로 JWT를 발급한다")
    void loginReturnsTokenForExistingMember() {
        SocialLoginRequest request = new SocialLoginRequest(
                "GOOGLE",
                "google-code",
                "http://localhost:5173/auth/callback"
        );
        Member member = memberWithId(1L, "google@example.com");
        OAuthUserInfo userInfo = new OAuthUserInfo(
                SocialProvider.GOOGLE,
                "google-123",
                "google@example.com",
                true
        );

        when(oAuthClients.authenticate(SocialProvider.GOOGLE, request.authorizationCode(), request.redirectUri()))
                .thenReturn(userInfo);
        when(memberRepository.findByEmail("google@example.com")).thenReturn(Optional.of(member));

        TokenResponse response = authService.login(request);

        assertThat(jwtTokenProvider.isValidAccessToken(response.accessToken())).isTrue();
        assertThat(jwtTokenProvider.isValidRefreshToken(response.refreshToken())).isTrue();
        assertThat(jwtTokenProvider.getMemberId(response.accessToken())).isEqualTo(1L);
    }

    @Test
    @DisplayName("소셜 로그인 시 처음 로그인한 이메일이면 회원을 생성한다")
    void loginCreatesMemberForNewEmail() {
        SocialLoginRequest request = new SocialLoginRequest(
                "GOOGLE",
                "google-code",
                "http://localhost:5173/auth/callback"
        );
        OAuthUserInfo userInfo = new OAuthUserInfo(
                SocialProvider.GOOGLE,
                "google-123",
                "new-google@example.com",
                true
        );
        Member savedMember = memberWithId(1L, "new-google@example.com");

        when(oAuthClients.authenticate(SocialProvider.GOOGLE, request.authorizationCode(), request.redirectUri()))
                .thenReturn(userInfo);
        when(memberRepository.save(any())).thenReturn(savedMember);

        TokenResponse response = authService.login(request);

        assertThat(jwtTokenProvider.isValidAccessToken(response.accessToken())).isTrue();
        assertThat(jwtTokenProvider.isValidRefreshToken(response.refreshToken())).isTrue();
        assertThat(jwtTokenProvider.getMemberId(response.accessToken())).isEqualTo(1L);
    }

    @Test
    @DisplayName("이메일 인증이 안 된 소셜 계정이면 로그인에 실패한다")
    void loginRejectsUnverifiedEmail() {
        SocialLoginRequest request = new SocialLoginRequest(
                "GOOGLE",
                "google-code",
                "http://localhost:5173/auth/callback"
        );
        OAuthUserInfo userInfo = new OAuthUserInfo(
                SocialProvider.GOOGLE,
                "google-123",
                "google@example.com",
                false
        );

        when(oAuthClients.authenticate(SocialProvider.GOOGLE, request.authorizationCode(), request.redirectUri()))
                .thenReturn(userInfo);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Social account email is not verified");
    }

    @Test
    @DisplayName("지원하지 않는 소셜 제공자로 로그인하면 실패한다")
    void loginRejectsUnsupportedProvider() {
        SocialLoginRequest request = new SocialLoginRequest(
                "UNKNOWN",
                "google-code",
                "http://localhost:5173/auth/callback"
        );

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Unsupported social provider");
    }

    @Test
    @DisplayName("리프레시 토큰이 DB에 존재하고 유효해야 재발급할 수 있다")
    void refreshReturnsNewTokenPair() {
        MemberPrincipal principal = new MemberPrincipal(1L);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);
        Member member = memberWithId(1L, "login@example.com");
        RefreshToken storedRefreshToken = new RefreshToken(
                member,
                refreshToken,
                LocalDateTime.now().plusDays(1)
        );

        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(storedRefreshToken));
        when(refreshTokenRepository.findByMember_IdOrderByUpdatedAtDescIdDesc(1L))
                .thenReturn(List.of(storedRefreshToken));

        TokenResponse response = authService.refresh(new RefreshTokenRequest(refreshToken));

        assertThat(jwtTokenProvider.isValidAccessToken(response.accessToken())).isTrue();
        assertThat(jwtTokenProvider.isValidRefreshToken(response.refreshToken())).isTrue();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(storedRefreshToken.getToken()).isEqualTo(response.refreshToken());
        assertThat(storedRefreshToken.getExpiredAt()).isAfter(LocalDateTime.now().plusDays(6));
    }

    @Test
    @DisplayName("DB에 없는 리프레시 토큰이면 예외가 발생한다")
    void refreshRejectsMissingRefreshToken() {
        MemberPrincipal principal = new MemberPrincipal(1L);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(refreshToken)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Refresh token is invalid");
    }

    @Test
    @DisplayName("만료된 리프레시 토큰이면 삭제 후 예외가 발생한다")
    void refreshRejectsExpiredRefreshToken() {
        MemberPrincipal principal = new MemberPrincipal(1L);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);
        Member member = memberWithId(1L, "login@example.com");
        RefreshToken expiredRefreshToken = new RefreshToken(
                member,
                refreshToken,
                LocalDateTime.now().minusMinutes(1)
        );

        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(expiredRefreshToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(refreshToken)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Refresh token is invalid");
        verify(refreshTokenRepository).delete(expiredRefreshToken);
    }

    @Test
    @DisplayName("새 세션 발급 시 최근 사용 2세션만 유지한다")
    void issueTokenResponseKeepsOnlyTwoLatestRefreshTokens() {
        MemberPrincipal principal = new MemberPrincipal(1L);
        Member member = memberWithId(1L, "login@example.com");
        RefreshToken first = new RefreshToken(member, "first-refresh-token", LocalDateTime.now().plusDays(1));
        RefreshToken second = new RefreshToken(member, "second-refresh-token", LocalDateTime.now().plusDays(1));
        RefreshToken third = new RefreshToken(member, "third-refresh-token", LocalDateTime.now().plusDays(1));

        when(memberRepository.getReferenceById(1L)).thenReturn(member);
        when(refreshTokenRepository.findByMember_IdOrderByUpdatedAtDescIdDesc(1L))
                .thenReturn(List.of(first, second, third));

        TokenResponse response = authService.issueTokenResponse(principal);

        assertThat(jwtTokenProvider.isValidAccessToken(response.accessToken())).isTrue();
        assertThat(jwtTokenProvider.isValidRefreshToken(response.refreshToken())).isTrue();
        verify(refreshTokenRepository).deleteAll(List.of(third));
    }

    @Test
    @DisplayName("현재 회원 조회 시 회원이 없으면 예외가 발생한다")
    void getCurrentMemberRejectsMissingMember() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentMember(99L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Member not found");
    }

    @Test
    @DisplayName("현재 회원 조회 시 회원 정보를 반환한다")
    void getCurrentMemberReturnsMember() {
        Member member = memberWithId(1L, "me@example.com");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberResponse response = authService.getCurrentMember(1L);

        assertThat(response.email()).isEqualTo("me@example.com");
    }

    private JwtProperties jwtProperties(long accessExpirationSeconds, long refreshExpirationSeconds) {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-test-secret-key-1234");
        jwtProperties.setAccessTokenExpirationSeconds(accessExpirationSeconds);
        jwtProperties.setRefreshTokenExpirationSeconds(refreshExpirationSeconds);
        return jwtProperties;
    }

    private Member memberWithId(Long id, String email) {
        Member member = Member.of(email);
        // Member id는 영속화 후 생성되므로 서비스 단위 테스트에서만 보강한다.
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

}
