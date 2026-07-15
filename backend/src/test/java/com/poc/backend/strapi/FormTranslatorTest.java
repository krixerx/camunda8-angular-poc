package com.poc.backend.strapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class FormTranslatorTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** Trimmed copy of the deployed vehicle-registration start form. */
  private static final String SCHEMA =
      """
      {
        "id": "vehicle-registration-start",
        "type": "default",
        "components": [
          { "type": "text", "id": "intro", "text": "## Vehicle registration" },
          {
            "type": "textfield", "id": "field-ownerName", "key": "ownerName",
            "label": "Owner name", "validate": { "required": true, "minLength": 2 }
          },
          {
            "type": "textfield", "id": "field-vin", "key": "vin",
            "label": "VIN", "description": "17 characters",
            "validate": { "required": true, "pattern": "[A-HJ-NPR-Z0-9]{17}" }
          },
          {
            "type": "select", "id": "field-category", "key": "category",
            "label": "Vehicle category",
            "values": [
              { "label": "Car", "value": "car" },
              { "label": "Motorcycle", "value": "motorcycle" }
            ],
            "validate": { "required": true }
          }
        ]
      }
      """;

  private static final String STRINGS =
      """
      {
        "intro": { "text": "## تسجيل مركبة" },
        "ownerName": { "label": "اسم المالك" },
        "vin": { "label": "رقم تعريف المركبة (VIN)", "description": "17 خانة" },
        "category": { "label": "فئة المركبة", "values": { "car": "سيارة" } }
      }
      """;

  @Test
  void translatesPresentationalStringsOnly() {
    StrapiClient strapiClient = mock(StrapiClient.class);
    when(strapiClient.fetchFormStrings("vehicle-registration-start", "ar"))
        .thenReturn(MAPPER.readTree(STRINGS));
    JsonNode schema = MAPPER.readTree(SCHEMA);

    JsonNode translated = new FormTranslator(strapiClient).translate(schema, "ar");

    // presentational strings are replaced
    assertThat(translated.at("/components/0/text").stringValue()).isEqualTo("## تسجيل مركبة");
    assertThat(translated.at("/components/1/label").stringValue()).isEqualTo("اسم المالك");
    assertThat(translated.at("/components/2/label").stringValue())
        .isEqualTo("رقم تعريف المركبة (VIN)");
    assertThat(translated.at("/components/2/description").stringValue()).isEqualTo("17 خانة");
    assertThat(translated.at("/components/3/values/0/label").stringValue()).isEqualTo("سيارة");
    // untranslated option keeps its authored label
    assertThat(translated.at("/components/3/values/1/label").stringValue())
        .isEqualTo("Motorcycle");
    // structure and behavior are untouched
    assertThat(translated.at("/components/1/key").stringValue()).isEqualTo("ownerName");
    assertThat(translated.at("/components/1/type").stringValue()).isEqualTo("textfield");
    assertThat(translated.at("/components/1/validate")).isEqualTo(schema.at("/components/1/validate"));
    assertThat(translated.at("/components/2/validate/pattern").stringValue())
        .isEqualTo("[A-HJ-NPR-Z0-9]{17}");
    assertThat(translated.at("/components/3/values/0/value").stringValue()).isEqualTo("car");
    // the input schema was not mutated
    assertThat(schema.at("/components/1/label").stringValue()).isEqualTo("Owner name");
  }

  @Test
  void untranslatedFieldKeepsAuthoredText() {
    StrapiClient strapiClient = mock(StrapiClient.class);
    when(strapiClient.fetchFormStrings(anyString(), eq("ar")))
        .thenReturn(MAPPER.readTree("{\"ownerName\":{\"label\":\"اسم المالك\"}}"));

    JsonNode translated =
        new FormTranslator(strapiClient).translate(MAPPER.readTree(SCHEMA), "ar");

    assertThat(translated.at("/components/1/label").stringValue()).isEqualTo("اسم المالك");
    assertThat(translated.at("/components/2/label").stringValue()).isEqualTo("VIN");
    assertThat(translated.at("/components/0/text").stringValue())
        .isEqualTo("## Vehicle registration");
  }

  @Test
  void missingTranslationReturnsSchemaUnchanged() {
    StrapiClient strapiClient = mock(StrapiClient.class);
    when(strapiClient.fetchFormStrings(anyString(), anyString())).thenReturn(null);
    JsonNode schema = MAPPER.readTree(SCHEMA);

    assertThat(new FormTranslator(strapiClient).translate(schema, "ar")).isSameAs(schema);
  }

  @Test
  void nullSchemaPassesThrough() {
    assertThat(new FormTranslator(mock(StrapiClient.class)).translate(null, "ar")).isNull();
  }

  @Test
  void englishBypassesStrapiInsideClient() {
    // the client returns null for the default locale without an HTTP call; the
    // translator then hands the schema back as-is
    StrapiClient strapiClient = mock(StrapiClient.class);
    when(strapiClient.fetchFormStrings(anyString(), eq(null))).thenReturn(null);
    JsonNode schema = MAPPER.readTree(SCHEMA);

    JsonNode result = new FormTranslator(strapiClient).translate(schema, null);

    assertThat(result).isSameAs(schema);
    verify(strapiClient, never()).fetchFormStrings(anyString(), eq("ar"));
  }
}
