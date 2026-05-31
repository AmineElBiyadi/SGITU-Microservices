package ma.sgitu.g5.config;

import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @PostConstruct
    public void init() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            if (credentialsPath == null || credentialsPath.isBlank()) {
                log.warn("[FCM] FIREBASE_CREDENTIALS_PATH non defini. FCM desactive.");
                return;
            }
            try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
                FirebaseApp.initializeApp(options);
                log.info("[FCM] Firebase initialise avec les credentials: {}", credentialsPath);
            }
        }
    }
}
