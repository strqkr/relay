package com.gesmio.relay.web;

import com.gesmio.relay.domain.Endpoint;
import com.gesmio.relay.repository.EndpointRepository;
import com.gesmio.relay.signing.HmacSigner;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/endpoints")
public class EndpointController {

    private final EndpointRepository endpointRepository;
    private final HmacSigner hmacSigner;

    public EndpointController(EndpointRepository endpointRepository, HmacSigner hmacSigner) {
        this.endpointRepository = endpointRepository;
        this.hmacSigner = hmacSigner;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointResponse create(@Valid @RequestBody CreateEndpointRequest request) {
        Endpoint endpoint = new Endpoint(request.name(), request.url(), hmacSigner.generateSecret());
        if (request.rateLimitPerSecond() != null) {
            endpoint.setRateLimitPerSecond(request.rateLimitPerSecond());
        }
        return EndpointResponse.from(endpointRepository.save(endpoint));
    }

    @GetMapping("/{id}")
    public EndpointResponse get(@PathVariable Long id) {
        return endpointRepository.findById(id)
                .map(EndpointResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "endpoint not found"));
    }
}
