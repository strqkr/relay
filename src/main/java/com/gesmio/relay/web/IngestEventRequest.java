package com.gesmio.relay.web;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IngestEventRequest(
        @NotBlank String type,
        @NotNull JsonNode payload
) {
}
