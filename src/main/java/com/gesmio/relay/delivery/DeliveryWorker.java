package com.gesmio.relay.delivery;

import com.gesmio.relay.domain.Delivery;
import com.gesmio.relay.domain.DeliveryStatus;
import com.gesmio.relay.domain.Endpoint;
import com.gesmio.relay.domain.Event;
import com.gesmio.relay.ratelimit.RateLimiterService;
import com.gesmio.relay.repository.DeliveryRepository;
import com.gesmio.relay.signing.HmacSigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.List;

@Component
public class DeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(DeliveryWorker.class);

    private final DeliveryRepository deliveryRepository;
    private final HmacSigner hmacSigner;
    private final RestClient restClient;
    private final BackoffCalculator backoffCalculator;
    private final RateLimiterService rateLimiterService;

    public DeliveryWorker(DeliveryRepository deliveryRepository, HmacSigner hmacSigner,
                           RestClient restClient, BackoffCalculator backoffCalculator,
                           RateLimiterService rateLimiterService) {
        this.deliveryRepository = deliveryRepository;
        this.hmacSigner = hmacSigner;
        this.restClient = restClient;
        this.backoffCalculator = backoffCalculator;
        this.rateLimiterService = rateLimiterService;
    }

    @Scheduled(fixedDelayString = "${relay.worker.poll-interval-ms:5000}")
    @Transactional
    public void processDueDeliveries() {
        List<Delivery> due = deliveryRepository.findByStatusAndNextAttemptAtLessThanEqual(DeliveryStatus.PENDING, Instant.now());
        due.forEach(this::attempt);
    }

    @Transactional
    public void attempt(Delivery delivery) {
        Event event = delivery.getEvent();
        Endpoint endpoint = event.getEndpoint();

        if (!rateLimiterService.tryConsume(endpoint.getId(), endpoint.getRateLimitPerSecond())) {
            delivery.setNextAttemptAt(Instant.now().plusSeconds(1));
            deliveryRepository.save(delivery);
            return;
        }

        String payload = event.getPayload();
        String signature = hmacSigner.sign(payload, endpoint.getSecret());

        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setLastAttemptAt(Instant.now());

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(endpoint.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HmacSigner.SIGNATURE_HEADER, signature)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            delivery.setLastResponseStatus(response.getStatusCode().value());
            delivery.setStatus(DeliveryStatus.SUCCESS);
        } catch (RestClientResponseException e) {
            delivery.setLastResponseStatus(e.getStatusCode().value());
            scheduleRetryOrFail(delivery);
        } catch (RestClientException e) {
            log.warn("Delivery {} attempt {} failed: {}", delivery.getId(), delivery.getAttemptCount(), e.getMessage());
            delivery.setLastResponseStatus(null);
            scheduleRetryOrFail(delivery);
        }

        deliveryRepository.save(delivery);
    }

    private void scheduleRetryOrFail(Delivery delivery) {
        if (delivery.getAttemptCount() >= delivery.getMaxAttempts()) {
            delivery.setStatus(DeliveryStatus.FAILED);
        } else {
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setNextAttemptAt(Instant.now().plus(backoffCalculator.delayFor(delivery.getAttemptCount())));
        }
    }
}
