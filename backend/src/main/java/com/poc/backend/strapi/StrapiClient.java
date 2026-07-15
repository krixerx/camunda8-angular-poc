package com.poc.backend.strapi;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Read-only client for the Strapi content API. The catalog must never fail because the CMS is
 * down: any transport or parse problem degrades to "no content" (empty map) with a warning, and
 * the short timeouts keep a dead Strapi from stalling the request path.
 */
@Component
public class StrapiClient {

  private static final Logger log = LoggerFactory.getLogger(StrapiClient.class);
  private static final Duration TIMEOUT = Duration.ofSeconds(2);

  private final RestClient restClient;

  @Autowired
  public StrapiClient(RestClient.Builder builder, @Value("${strapi.base-url}") String baseUrl) {
    this(builder.baseUrl(baseUrl).requestFactory(timeoutBoundRequestFactory()).build());
  }

  StrapiClient(RestClient restClient) {
    this.restClient = restClient;
  }

  /**
   * Published service entries keyed by {@code processDefinitionId}. Empty when Strapi is
   * unreachable, times out, or returns an unexpected payload.
   */
  public Map<String, StrapiService> fetchServicesById() {
    try {
      ServiceListResponse response =
          restClient
              .get()
              .uri(uri -> uri.path("/api/services").queryParam("pagination[pageSize]", 100).build())
              .retrieve()
              .body(ServiceListResponse.class);
      if (response == null || response.data() == null) {
        return Map.of();
      }
      return response.data().stream()
          .filter(s -> s.processDefinitionId() != null)
          .collect(
              Collectors.toMap(
                  StrapiService::processDefinitionId, Function.identity(), (a, b) -> a));
    } catch (RestClientException e) {
      log.warn("Strapi unavailable ({}); serving engine-only catalog", e.getMessage());
      return Map.of();
    }
  }

  private static JdkClientHttpRequestFactory timeoutBoundRequestFactory() {
    var factory =
        new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    factory.setReadTimeout(TIMEOUT);
    return factory;
  }

  /** Strapi v5 list envelope; the {@code meta} pagination block is irrelevant here. */
  private record ServiceListResponse(List<StrapiService> data) {}
}
