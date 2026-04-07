package com.dodo.todo.auth.social.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dodo.todo.auth.social.domain.OAuthUserInfo;
import com.dodo.todo.auth.social.domain.SocialProvider;
import com.dodo.todo.common.exception.ApiException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuthClientsTest {

    @Mock
    private OAuthClient oAuthClient;

    @Test
    @DisplayName("지원하는 소셜 제공자의 OAuth 클라이언트로 인증을 위임한다")
    void authenticateDelegatesToSupportedOAuthClient() {
        OAuthClients oAuthClients = new OAuthClients(List.of(oAuthClient));
        OAuthUserInfo userInfo = new OAuthUserInfo(
                SocialProvider.GOOGLE,
                "google-123",
                "google@example.com",
                true
        );

        when(oAuthClient.supports(SocialProvider.GOOGLE)).thenReturn(true);
        when(oAuthClient.authenticate("google-code", "http://localhost:5173/auth/callback"))
                .thenReturn(userInfo);

        OAuthUserInfo response = oAuthClients.authenticate(
                SocialProvider.GOOGLE,
                "google-code",
                "http://localhost:5173/auth/callback"
        );

        assertThat(response).isEqualTo(userInfo);
        verify(oAuthClient).authenticate("google-code", "http://localhost:5173/auth/callback");
    }

    @Test
    @DisplayName("지원하는 OAuth 클라이언트가 없으면 예외가 발생한다")
    void authenticateRejectsUnsupportedOAuthClient() {
        OAuthClients oAuthClients = new OAuthClients(List.of(oAuthClient));

        when(oAuthClient.supports(SocialProvider.GOOGLE)).thenReturn(false);

        assertThatThrownBy(() -> oAuthClients.authenticate(
                SocialProvider.GOOGLE,
                "google-code",
                "http://localhost:5173/auth/callback"
        ))
                .isInstanceOf(ApiException.class)
                .hasMessage("Unsupported social provider");
    }
}
