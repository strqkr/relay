package com.gesmio.relay.web;

import com.gesmio.relay.domain.Delivery;
import com.gesmio.relay.domain.DeliveryStatus;
import com.gesmio.relay.repository.DeliveryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private final DeliveryRepository deliveryRepository;

    public DeliveryController(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Page<DeliveryResponse> list(@RequestParam(required = false) DeliveryStatus status, Pageable pageable) {
        Page<Delivery> page = status != null
                ? deliveryRepository.findByStatus(status, pageable)
                : deliveryRepository.findAll(pageable);
        return page.map(DeliveryResponse::from);
    }

    @PostMapping("/{id}/replay")
    @Transactional
    public DeliveryResponse replay(@PathVariable Long id) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "delivery not found"));

        if (delivery.getStatus() != DeliveryStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "only failed deliveries can be replayed");
        }

        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setAttemptCount(0);
        delivery.setNextAttemptAt(Instant.now());

        return DeliveryResponse.from(deliveryRepository.save(delivery));
    }
}
