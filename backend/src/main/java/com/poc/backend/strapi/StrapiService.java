package com.poc.backend.strapi;

/**
 * One published entry of the Strapi {@code service} collection type (v5 REST shape — attributes
 * arrive flattened on the data item). Field names mirror the schema in
 * {@code cms/src/api/service/content-types/service/schema.json}.
 */
public record StrapiService(
    String title,
    String summary,
    String instructions,
    String whatYouNeed,
    String expectedDuration,
    String processDefinitionId) {}
