package com.safra.safra.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        // Load Firebase service account credentials
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new ClassPathResource("firebase-service-account.json")
                        .getInputStream());

        // Configure Firebase options
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        // Initialize Firebase app
        FirebaseApp app = FirebaseApp.initializeApp(options);

        // Return FirebaseMessaging instance
        return FirebaseMessaging.getInstance(app);
    }
}