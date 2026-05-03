package com.dodo.todo.auth.social.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.dodo.todo.auth.social.domain.OAuthUserInfo;
import com.dodo.todo.auth.social.domain.SocialProvider;
import com.dodo.todo.common.exception.BusinessException;
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
        when(oAuthClient.authenticate("google-access-token")).thenReturn(userInfo);

        OAuthUserInfo response = oAuthClients.authenticate(SocialProvider.GOOGLE, "google-access-token");

        assertThat(response).isEqualTo(userInfo);
    }

    @Test
    @DisplayName("지원하는 OAuth 클라이언트가 없으면 예외가 발생한다")
    void authenticateRejectsUnsupportedOAuthClient() {
        OAuthClients oAuthClients = new OAuthClients(List.of(oAuthClient));

        when(oAuthClient.supports(SocialProvider.GOOGLE)).thenReturn(false);

        assertThatThrownBy(() -> oAuthClients.authenticate(SocialProvider.GOOGLE, "google-access-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Unsupported social provider");
    }
}
