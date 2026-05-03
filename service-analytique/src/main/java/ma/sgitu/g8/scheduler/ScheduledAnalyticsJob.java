package ma.sgitu.g8.scheduler;

import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g8.aggregation.IncidentAggregation;
import ma.sgitu.g8.aggregation.RevenueAggregation;
import ma.sgitu.g8.aggregation.SubscriptionAggregation;
import ma.sgitu.g8.aggregation.TicketAggregation;
import ma.sgitu.g8.aggregation.VehicleAggregation;
import ma.sgitu.g8.alert.ThresholdAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ScheduledAnalyticsJob {

    @Autowired
    private IncidentAggregation incidentAggregation;

    @Autowired
    private VehicleAggregation vehicleAggregation;

    @Autowired
    private TicketAggregation ticketAggregation;

    @Autowired
    private RevenueAggregation revenueAggregation;

    @Autowired
    private SubscriptionAggregation subscriptionAggregation;

    @Autowired
    private ThresholdAlertService thresholdAlertService;

    @Scheduled(fixedRate = 60000)
    public void runAnalytics() {
        log.info("ScheduledAnalyticsJob started");
        incidentAggregation.compute();
        vehicleAggregation.compute();
        ticketAggregation.compute();
        revenueAggregation.compute();
        subscriptionAggregation.compute();
        thresholdAlertService.detect();
        log.info("ScheduledAnalyticsJob finished");
    }
}
