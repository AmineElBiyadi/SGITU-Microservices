package ma.sgitu.g5.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration du TaskScheduler Spring utilisé par RetryServiceImpl.
 *
 * Le TaskScheduler est préféré à Thread.sleep car :
 * - Il n'occupe pas de thread pendant l'attente (scheduled dans un pool dédié)
 * - Il est géré par le contexte Spring (shutdown propre, métriques)
 * - Il permet un backoff exponentiel précis (scheduleRetry avec Instant)
 */
@Configuration
public class SchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);  // 5 threads pour gérer les retries parallèles
        scheduler.setThreadNamePrefix("g5-retry-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }
}
