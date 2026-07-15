package com.poc.backend.strapi;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Overlays Strapi form translations onto a Camunda Form schema at read time. Strictly
 * presentational: only {@code label}, {@code description}, {@code placeholder}, select/radio
 * option labels, and text-view {@code text} are replaced — never {@code key}, {@code type},
 * validation, conditionals, or FEEL expressions. Anything missing (translation entry, single
 * string, Strapi itself) leaves the authored English value in place.
 */
@Component
public class FormTranslator {

  private static final String[] TRANSLATABLE_PROPS = {"label", "description", "placeholder", "text"};

  private final StrapiClient strapiClient;

  public FormTranslator(StrapiClient strapiClient) {
    this.strapiClient = strapiClient;
  }

  /**
   * Returns the schema with the locale's translations applied, or the schema untouched for the
   * default locale, unknown forms, or an unreachable CMS. The input node is never mutated.
   */
  public JsonNode translate(JsonNode schema, String locale) {
    if (schema == null || !schema.isObject()) {
      return schema;
    }
    String formId = stringOrNull(schema.path("id"));
    if (formId == null) {
      return schema;
    }
    JsonNode strings = strapiClient.fetchFormStrings(formId, locale);
    if (strings == null || !strings.isObject()) {
      return schema;
    }
    JsonNode copy = schema.deepCopy();
    translateComponents(copy.path("components"), strings);
    return copy;
  }

  private void translateComponents(JsonNode components, JsonNode strings) {
    if (!components.isArray()) {
      return;
    }
    for (JsonNode child : components) {
      if (!(child instanceof ObjectNode component)) {
        continue;
      }
      // fields are addressed by their process-variable key, text views by their id
      String lookup = stringOrNull(component.path("key"));
      if (lookup == null) {
        lookup = stringOrNull(component.path("id"));
      }
      JsonNode entry = lookup != null ? strings.path(lookup) : null;
      if (entry != null && entry.isObject()) {
        for (String prop : TRANSLATABLE_PROPS) {
          String translated = stringOrNull(entry.path(prop));
          if (translated != null) {
            component.put(prop, translated);
          }
        }
        translateOptionLabels(component.path("values"), entry.path("values"));
      }
      translateComponents(component.path("components"), strings); // nested groups
    }
  }

  /** Option labels are matched by option {@code value} — the value itself is never changed. */
  private void translateOptionLabels(JsonNode options, JsonNode translations) {
    if (!options.isArray() || !translations.isObject()) {
      return;
    }
    for (JsonNode option : options) {
      if (option instanceof ObjectNode optionNode) {
        String value = stringOrNull(optionNode.path("value"));
        String translated = value != null ? stringOrNull(translations.path(value)) : null;
        if (translated != null) {
          optionNode.put("label", translated);
        }
      }
    }
  }

  private static String stringOrNull(JsonNode node) {
    return node.isString() ? node.stringValue() : null;
  }
}
