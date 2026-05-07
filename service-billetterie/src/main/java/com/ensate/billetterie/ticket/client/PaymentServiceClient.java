package com.ensate.billetterie.ticket.client;


import com.ensate.billetterie.ticket.dto.request.PaymentRequest;
import com.ensate.billetterie.ticket.dto.response.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class PaymentServiceClient {
    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    public PaymentServiceClient(
            RestTemplate restTemplate,
            @Value("${payment.service.url}") String paymentServiceUrl) {

        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;

        log.info("Connecting to Payment Service REST API at {}",
                paymentServiceUrl);
    }

    public PaymentResponse pay(PaymentRequest paymentRequest) {


        PaymentResponse response = restTemplate.postForObject(
                paymentServiceUrl + "/payment",
                paymentRequest,
                PaymentResponse.class
        );

        log.info("Received response from billing service via REST: {}",
                response);

        return response;
    }
}
