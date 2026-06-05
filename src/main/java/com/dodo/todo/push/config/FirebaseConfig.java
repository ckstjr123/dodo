package com.dodo.todo.push.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.IOException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Firebase Messaging 초기화
     * 설정된 서비스 계정 리소스로 FirebaseApp을 초기화한다.
     */
    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseProperties properties) throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(loadCredentials(properties))
                    .build();
            FirebaseApp.initializeApp(options);
        }

        return FirebaseMessaging.getInstance();
    }

    private GoogleCredentials loadCredentials(FirebaseProperties properties) throws IOException {
        Resource resource = resourceLoader.getResource(properties.getServiceAccountPath());
        try (var inputStream = resource.getInputStream()) {
            return GoogleCredentials.fromStream(inputStream);
        }
    }
}
