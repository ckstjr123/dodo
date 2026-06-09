package com.dodo.todo.auth.social.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import com.dodo.todo.auth.social.domain.SocialProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

class GoogleApiAuthClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Google access token으로 사용자 정보를 조회한다")
    void authenticateReturnsOAuthUserInfo() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        GoogleApiAuthClient client = new GoogleApiAuthClient(restTemplate);

        server.expect(once(), requestTo(GoogleApiAuthClient.GOOGLE_USER_INFO_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer google-access-token"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(userInfoResponse()), MediaType.APPLICATION_JSON));

        var userInfo = client.authenticate("google-access-token");

        assertThat(userInfo.provider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(userInfo.providerUserId()).isEqualTo("google-123");
        assertThat(userInfo.email()).isEqualTo("google@example.com");
        assertThat(userInfo.emailVerified()).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("Google userinfo 조회가 실패하면 RestTemplate 예외를 전달한다")
    void authenticateThrowsRestClientExceptionWhenGoogleCallFails() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        GoogleApiAuthClient client = new GoogleApiAuthClient(restTemplate);

        server.expect(once(), requestTo(GoogleApiAuthClient.GOOGLE_USER_INFO_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer bad-access-token"))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> client.authenticate("bad-access-token"))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
        server.verify();
    }

    private GoogleApiAuthClient.GoogleUserInfoResponse userInfoResponse() {
        return new GoogleApiAuthClient.GoogleUserInfoResponse(
                "google-123",
                "google@example.com",
                true
        );
    }

}
