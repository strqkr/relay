package com.gesmio.relay.security;

import com.gesmio.relay.domain.Organization;
import com.gesmio.relay.repository.OrganizationRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String ORGANIZATION_ATTRIBUTE = "currentOrganization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> PUBLIC_PATHS = Set.of("/organizations");

    private final OrganizationRepository organizationRepository;
    private final ApiKeyHasher apiKeyHasher;

    public ApiKeyAuthFilter(OrganizationRepository organizationRepository, ApiKeyHasher apiKeyHasher) {
        this.organizationRepository = organizationRepository;
        this.apiKeyHasher = apiKeyHasher;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_PATHS.contains(request.getRequestURI()) || request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "missing or malformed Authorization header");
            return;
        }

        String rawKey = header.substring(BEARER_PREFIX.length());
        Optional<Organization> organization = organizationRepository.findByApiKeyHash(apiKeyHasher.hash(rawKey));
        if (organization.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid api key");
            return;
        }

        request.setAttribute(ORGANIZATION_ATTRIBUTE, organization.get());
        filterChain.doFilter(request, response);
    }
}
