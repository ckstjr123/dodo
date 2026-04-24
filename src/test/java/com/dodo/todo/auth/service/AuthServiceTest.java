package com.dodo.todo.auth.service;

import static com.dodo.todo.util.TestFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dodo.todo.auth.config.JwtProperties;
import com.dodo.todo.auth.domain.RefreshToken;
import com.dodo.todo.auth.dto.MemberResponse;
import com.dodo.todo.auth.dto.RefreshTokenRequest;
import com.dodo.todo.auth.dto.SocialLoginRequest;
import com.dodo.todo.auth.dto.TokenResponse;
import com.dodo.todo.auth.jwt.JwtTokenProvider;
import com.dodo.todo.auth.principal.MemberPrincipal;
import com.dodo.todo.auth.repository.RefreshTokenRepository;
import com.dodo.todo.auth.social.client.OAuthClients;
import com.dodo.todo.auth.social.domain.OAuthUserInfo;
import com.dodo.todo.auth.social.domain.SocialProvider;
import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.repository.MemberRepository;
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
    @DisplayName("기존 회원으로 소셜 로그인하면 JWT를 반환한다")
    void loginReturnsTokenForExistingMember() {
        Long memberId = 1L;
        String email = "google@example.com";
        SocialLoginRequest request = new SocialLoginRequest(
                "GOOGLE",
                "google-code",
                "http://localhost:5173/auth/callback"
        );
        Member member = createMember(memberId, email);
        OAuthUserInfo userInfo = new OAuthUserInfo(SocialProvider.GOOGLE, "google-123", email, true);

        when(oAuthClients.authenticate(SocialProvider.GOOGLE, request.authorizationCode(), request.redirectUri()))
                .thenReturn(userInfo);
        when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));

        TokenResponse response = authService.login(request);

        assertThat(jwtTokenProvider.isValidAccessToken(response.accessToken())).isTrue();
        assertThat(jwtTokenProvider.isValidRefreshToken(response.refreshToken())).isTrue();
        assertThat(jwtTokenProvider.getMemberId(response.accessToken())).isEqualTo(memberId);
    }

    @Test
    @DisplayName("신규 이메일이면 회원을 생성하고 토큰을 반환한다")
    void loginCreatesMemberForNewEmail() {
        Long memberId = 1L;
        String email = "new-google@example.com";
        SocialLoginRequest request = new SocialLoginRequest(
                "GOOGLE",
                "google-code",
                "http://localhost:5173/auth/callback"
        );
        OAuthUserInfo userInfo = new OAuthUserInfo(SocialProvider.GOOGLE, "google-123", email, true);
        Member savedMember = createMember(memberId, email);

        when(oAuthClients.authenticate(SocialProvider.GOOGLE, request.authorizationCode(), request.redirectUri()))
                .thenReturn(userInfo);
        when(memberRepository.save(any())).thenReturn(savedMember);

        TokenResponse response = authService.login(request);

        assertThat(jwtTokenProvider.isValidAccessToken(response.accessToken())).isTrue();
        assertThat(jwtTokenProvider.isValidRefreshToken(response.refreshToken())).isTrue();
        assertThat(jwtTokenProvider.getMemberId(response.accessToken())).isEqualTo(memberId);
    }

    @Test
    @DisplayName("이메일 인증이 안 된 소셜 계정은 로그인할 수 없다")
    void loginRejectsUnverifiedEmail() {
        String expectedMessage = "Social account email is not verified";
        SocialLoginRequest request = new SocialLoginRequest(
                "GOOGLE",
                "google-code",
                "http://localhost:5173/auth/callback"
        );
        OAuthUserInfo userInfo = new OAuthUserInfo(SocialProvider.GOOGLE, "google-123", "google@example.com", false);

        when(oAuthClients.authenticate(SocialProvider.GOOGLE, request.authorizationCode(), request.redirectUri()))
                .thenReturn(userInfo);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ApiException.class)
                .hasMessage(expectedMessage);
    }

    @Test
    @DisplayName("지원하지 않는 소셜 제공자는 예외가 발생한다")
    void loginRejectsUnsupportedProvider() {
        String expectedMessage = "Unsupported social provider";
        SocialLoginRequest request = new SocialLoginRequest(
                "UNKNOWN",
                "google-code",
                "http://localhost:5173/auth/callback"
        );

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ApiException.class)
                .hasMessage(expectedMessage);
    }

    @Test
    @DisplayName("유효한 refresh token이면 새 토큰 쌍을 반환한다")
    void refreshReturnsNewTokenPair() {
        Long memberId = 1L;
        MemberPrincipal principal = new MemberPrincipal(memberId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);
        Member member = createMember(memberId, "login@example.com");
        RefreshToken storedRefreshToken = new RefreshToken(
                member,
                refreshToken,
                LocalDateTime.now().plusDays(1)
        );

        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(storedRefreshToken));
        when(refreshTokenRepository.findByMember_IdOrderByUpdatedAtDescIdDesc(memberId))
                .thenReturn(List.of(storedRefreshToken));

        TokenResponse response = authService.refresh(new RefreshTokenRequest(refreshToken));

        assertThat(jwtTokenProvider.isValidAccessToken(response.accessToken())).isTrue();
        assertThat(jwtTokenProvider.isValidRefreshToken(response.refreshToken())).isTrue();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(storedRefreshToken.getToken()).isEqualTo(response.refreshToken());
        assertThat(storedRefreshToken.getExpiredAt()).isAfter(LocalDateTime.now().plusDays(6));
    }

    @Test
    @DisplayName("DB에 없는 refresh token은 예외가 발생한다")
    void refreshRejectsMissingRefreshToken() {
        Long memberId = 1L;
        String expectedMessage = "Refresh token is invalid";
        String refreshToken = jwtTokenProvider.generateRefreshToken(new MemberPrincipal(memberId));

        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(refreshToken)))
                .isInstanceOf(ApiException.class)
                .hasMessage(expectedMessage);
    }

    @Test
    @DisplayName("만료된 refresh token은 예외가 발생한다")
    void refreshRejectsExpiredRefreshToken() {
        Long memberId = 1L;
        String expectedMessage = "Refresh token is invalid";
        String refreshToken = jwtTokenProvider.generateRefreshToken(new MemberPrincipal(memberId));
        Member member = createMember(memberId, "login@example.com");
        RefreshToken expiredRefreshToken = new RefreshToken(
                member,
                refreshToken,
                LocalDateTime.now().minusMinutes(1)
        );

        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(expiredRefreshToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(refreshToken)))
                .isInstanceOf(ApiException.class)
                .hasMessage(expectedMessage);
        verify(refreshTokenRepository).delete(expiredRefreshToken);
    }

    @Test
    @DisplayName("최신 refresh token 두 개만 유지한다")
    void issueTokenResponseKeepsOnlyTwoLatestRefreshTokens() {
        Long memberId = 1L;
        MemberPrincipal principal = new MemberPrincipal(memberId);
        Member member = createMember(memberId, "login@example.com");
        RefreshToken first = new RefreshToken(member, "first-refresh-token", LocalDateTime.now().plusDays(1));
        RefreshToken second = new RefreshToken(member, "second-refresh-token", LocalDateTime.now().plusDays(1));
        RefreshToken third = new RefreshToken(member, "third-refresh-token", LocalDateTime.now().plusDays(1));

        when(memberRepository.getReferenceById(memberId)).thenReturn(member);
        when(refreshTokenRepository.findByMember_IdOrderByUpdatedAtDescIdDesc(memberId))
                .thenReturn(List.of(first, second, third));

        TokenResponse response = authService.issueTokenResponse(principal);

        assertThat(jwtTokenProvider.isValidAccessToken(response.accessToken())).isTrue();
        assertThat(jwtTokenProvider.isValidRefreshToken(response.refreshToken())).isTrue();
        verify(refreshTokenRepository).deleteAll(List.of(third));
    }

    @Test
    @DisplayName("현재 회원 조회 시 회원이 없으면 예외가 발생한다")
    void getCurrentMemberRejectsMissingMember() {
        Long missingMemberId = 99L;
        String expectedMessage = "Member not found";
        when(memberRepository.findById(missingMemberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentMember(missingMemberId))
                .isInstanceOf(ApiException.class)
                .hasMessage(expectedMessage);
    }

    @Test
    @DisplayName("현재 회원 조회 시 회원 정보를 반환한다")
    void getCurrentMemberReturnsMember() {
        Long memberId = 1L;
        String email = "me@example.com";
        Member member = createMember(memberId, email);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        MemberResponse response = authService.getCurrentMember(memberId);

        assertThat(response.email()).isEqualTo(email);
    }

    private JwtProperties jwtProperties(long accessExpirationSeconds, long refreshExpirationSeconds) {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-test-secret-key-1234");
        jwtProperties.setAccessTokenExpirationSeconds(accessExpirationSeconds);
        jwtProperties.setRefreshTokenExpirationSeconds(refreshExpirationSeconds);
        return jwtProperties;
    }
}
