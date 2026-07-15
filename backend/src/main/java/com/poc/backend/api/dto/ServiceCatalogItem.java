package com.poc.backend.api.dto;

import com.poc.backend.strapi.StrapiService;
import io.camunda.client.api.search.response.ProcessDefinition;

/**
 * One catalog entry: engine identity from Camunda merged with optional editorial content from
 * Strapi. Content fields are null when the CMS has no matching entry (or is down) — the frontend
 * falls back to engine name/id.
 */
public record ServiceCatalogItem(
    long processDefinitionKey,
    String processDefinitionId,
    String name,
    int version,
    String title,
    String summary,
    String instructions,
    String whatYouNeed,
    String expectedDuration) {

  public static ServiceCatalogItem from(ProcessDefinition pd, StrapiService content) {
    return new ServiceCatalogItem(
        pd.getProcessDefinitionKey(),
        pd.getProcessDefinitionId(),
        pd.getName() != null ? pd.getName() : pd.getProcessDefinitionId(),
        pd.getVersion(),
        content != null ? content.title() : null,
        content != null ? content.summary() : null,
        content != null ? content.instructions() : null,
        content != null ? content.whatYouNeed() : null,
        content != null ? content.expectedDuration() : null);
  }
}
