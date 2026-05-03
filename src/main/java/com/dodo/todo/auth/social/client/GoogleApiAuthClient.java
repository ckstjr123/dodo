package com.dodo.todo.auth.social.client;

import com.dodo.todo.auth.social.domain.OAuthUserInfo;
import com.dodo.todo.auth.social.domain.SocialProvider;
import com.dodo.todo.common.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GoogleApiAuthClient implements OAuthClient {

    static final String GOOGLE_USER_INFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";

    private final RestTemplate restTemplate;

    @Autowired
    public GoogleApiAuthClient(RestTemplateBuilder restTemplateBuilder) {
        this(restTemplateBuilder.build());
    }

    GoogleApiAuthClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean supports(SocialProvider provider) {
        return provider == SocialProvider.GOOGLE;
    }

    /**
     * Google 인증
     * 전달받은 Google access token으로 사용자 정보를 조회한다.
     */
    @Override
    public OAuthUserInfo authenticate(String accessToken) {
        GoogleUserInfoResponse userInfoResponse = fetchUserInfo(accessToken);
        validateUserInfoResponse(userInfoResponse);

        return new OAuthUserInfo(
                SocialProvider.GOOGLE,
                userInfoResponse.sub(),
                userInfoResponse.email(),
                Boolean.TRUE.equals(userInfoResponse.emailVerified())
        );
    }

    private GoogleUserInfoResponse fetchUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<GoogleUserInfoResponse> response = restTemplate.exchange(
                GOOGLE_USER_INFO_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                GoogleUserInfoResponse.class
        );

        return response.getBody();
    }

    private void validateUserInfoResponse(GoogleUserInfoResponse userInfoResponse) {
        if (userInfoResponse == null) {
            throw new BusinessException(
                    "SOCIAL_AUTHENTICATION_FAILED",
                    HttpStatus.UNAUTHORIZED.value(),
                    "Social authentication failed"
            );
        }
    }

    record GoogleUserInfoResponse(
            String sub,
            String email,
            @JsonProperty("email_verified")
            Boolean emailVerified
    ) {
    }
}
