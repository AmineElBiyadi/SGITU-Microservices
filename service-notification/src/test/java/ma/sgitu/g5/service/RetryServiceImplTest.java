package ma.sgitu.g5.service;

import ma.sgitu.g5.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — RetryServiceImpl")
class RetryServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private TaskScheduler taskScheduler;

    @InjectMocks
    private RetryServiceImpl retryService;

    @Test
    @DisplayName("shouldRetry — 0,1,2 → true ; 3+ → false")
    void shouldRetry_backoffPolicy() {
        assertThat(retryService.shouldRetry(0)).isTrue();
        assertThat(retryService.shouldRetry(1)).isTrue();
        assertThat(retryService.shouldRetry(2)).isTrue();
        assertThat(retryService.shouldRetry(3)).isFalse();
    }

    @Test
    @DisplayName("nextDelaySeconds — backoff exponentiel 30s, 60s, 120s")
    void nextDelaySeconds_exponential() {
        assertThat(retryService.nextDelaySeconds(0)).isEqualTo(30);
        assertThat(retryService.nextDelaySeconds(1)).isEqualTo(60);
        assertThat(retryService.nextDelaySeconds(2)).isEqualTo(120);
    }
}
