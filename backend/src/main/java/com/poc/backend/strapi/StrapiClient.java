package com.poc.backend.strapi;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
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
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JsonNode;

/**
 * Read-only client for the Strapi content API. The catalog must never fail because the CMS is
 * down: any transport or parse problem degrades to "no content" (empty map / null) with a
 * warning, and the short timeouts keep a dead Strapi from stalling the request path.
 */
@Component
public class StrapiClient {

  /** The one non-default locale served by the CMS; anything else is treated as English. */
  public static final String LOCALE_ARABIC = "ar";

  private static final String LOCALE_DEFAULT = "en";
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

  /** Normalizes a raw request parameter: {@code ar} → Arabic, everything else → English. */
  public static String normalizeLocale(String locale) {
    return LOCALE_ARABIC.equalsIgnoreCase(locale) ? LOCALE_ARABIC : LOCALE_DEFAULT;
  }

  /** Published service entries keyed by {@code processDefinitionId} in the default locale. */
  public Map<String, StrapiService> fetchServicesById() {
    return fetchServicesById(LOCALE_DEFAULT);
  }

  /**
   * Published service entries keyed by {@code processDefinitionId} for the requested locale.
   * Non-default locales fall back per service to the default-locale entry when no localization
   * exists. Empty when Strapi is unreachable, times out, or returns an unexpected payload.
   */
  public Map<String, StrapiService> fetchServicesById(String locale) {
    String normalized = normalizeLocale(locale);
    if (LOCALE_DEFAULT.equals(normalized)) {
      return fetchServicesForLocale(null);
    }
    Map<String, StrapiService> merged = new HashMap<>(fetchServicesForLocale(null));
    merged.putAll(fetchServicesForLocale(normalized));
    return Map.copyOf(merged);
  }

  /**
   * The {@code strings} map of the published form translation for (formId, locale), or null when
   * there is none / Strapi is unreachable — the caller then serves the authored schema.
   */
  public JsonNode fetchFormStrings(String formId, String locale) {
    String normalized = normalizeLocale(locale);
    if (LOCALE_DEFAULT.equals(normalized)) {
      return null; // English is authored in the .form files; the CMS is never consulted
    }
    try {
      FormTranslationListResponse response =
          restClient
              .get()
              .uri(
                  uri ->
                      uri.path("/api/form-translations")
                          .queryParam("locale", normalized)
                          .queryParam("filters[formId][$eq]", formId)
                          .queryParam("pagination[pageSize]", 1)
                          .build())
              .retrieve()
              .body(FormTranslationListResponse.class);
      if (response == null || response.data() == null || response.data().isEmpty()) {
        return null;
      }
      return response.data().getFirst().strings();
    } catch (RestClientException e) {
      log.warn(
          "Strapi unavailable ({}); serving authored form schema for '{}'", e.getMessage(), formId);
      return null;
    }
  }

  private Map<String, StrapiService> fetchServicesForLocale(String localeOrNull) {
    try {
      ServiceListResponse response =
          restClient
              .get()
              .uri(uri -> servicesUri(uri, localeOrNull))
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

  private java.net.URI servicesUri(UriBuilder uri, String localeOrNull) {
    uri.path("/api/services").queryParam("pagination[pageSize]", 100);
    if (localeOrNull != null) {
      uri.queryParam("locale", localeOrNull);
    }
    return uri.build();
  }

  private static JdkClientHttpRequestFactory timeoutBoundRequestFactory() {
    var factory =
        new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    factory.setReadTimeout(TIMEOUT);
    return factory;
  }

  /** Strapi v5 list envelopes; the {@code meta} pagination block is irrelevant here. */
  private record ServiceListResponse(List<StrapiService> data) {}

  private record FormTranslationListResponse(List<FormTranslationEntry> data) {}

  private record FormTranslationEntry(String formId, JsonNode strings) {}
}
