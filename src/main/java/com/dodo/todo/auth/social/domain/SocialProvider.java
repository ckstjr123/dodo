package com.dodo.todo.auth.social.domain;

import com.dodo.todo.common.exception.ApiException;
import java.util.Arrays;
import org.springframework.http.HttpStatus;

public enum SocialProvider {
    GOOGLE;

    public static SocialProvider from(String provider) {
        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        "UNSUPPORTED_SOCIAL_PROVIDER",
                        HttpStatus.BAD_REQUEST,
                        "Unsupported social provider"
                ));
    }

    public String getRegistrationId() {
        return name().toLowerCase();
    }
}
