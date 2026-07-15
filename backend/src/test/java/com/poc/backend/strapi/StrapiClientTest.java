package com.poc.backend.strapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class StrapiClientTest {

  /** Strapi v5 flat list shape, including envelope fields the client must ignore. */
  private static final String V5_RESPONSE =
      """
      {
        "data": [
          {
            "id": 2,
            "documentId": "grvtu7ljii8bowfoq6jsg19i",
            "title": "Vehicle registration",
            "summary": "Register a vehicle.",
            "instructions": "Fill in the form.",
            "whatYouNeed": "- VIN",
            "expectedDuration": "1-2 working days",
            "processDefinitionId": "vehicle-registration",
            "createdAt": "2026-07-15T07:41:27.028Z",
            "updatedAt": "2026-07-15T07:41:27.028Z",
            "publishedAt": "2026-07-15T07:41:27.032Z"
          }
        ],
        "meta": { "pagination": { "page": 1, "pageSize": 100, "pageCount": 1, "total": 1 } }
      }
      """;

  @Test
  void mapsV5ListShapeKeyedByProcessDefinitionId() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://strapi.test");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo(Matchers.startsWith("http://strapi.test/api/services")))
        .andRespond(withSuccess(V5_RESPONSE, MediaType.APPLICATION_JSON));

    var services = new StrapiClient(builder.build()).fetchServicesById();

    assertThat(services).containsOnlyKeys("vehicle-registration");
    var vehicle = services.get("vehicle-registration");
    assertThat(vehicle.title()).isEqualTo("Vehicle registration");
    assertThat(vehicle.summary()).isEqualTo("Register a vehicle.");
    assertThat(vehicle.instructions()).isEqualTo("Fill in the form.");
    assertThat(vehicle.whatYouNeed()).isEqualTo("- VIN");
    assertThat(vehicle.expectedDuration()).isEqualTo("1-2 working days");
    server.verify();
  }

  @Test
  void degradesToEmptyMapWhenStrapiFails() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://strapi.test");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo(Matchers.startsWith("http://strapi.test/api/services")))
        .andRespond(withServerError());

    assertThat(new StrapiClient(builder.build()).fetchServicesById()).isEmpty();
  }

  @Test
  void arabicLocaleMergesWithEnglishFallback() {
    // English has both services, Arabic only vehicle-registration: the merged map
    // carries the Arabic entry where it exists and the English one elsewhere.
    String english =
        V5_RESPONSE.replace("\"Vehicle registration\"", "\"Vehicle registration\"")
            .replace(
                "\"data\": [",
                """
                "data": [
                  {
                    "id": 4, "documentId": "biz", "title": "Business registration",
                    "summary": "Register a company.", "instructions": null, "whatYouNeed": null,
                    "expectedDuration": null, "processDefinitionId": "business-registration",
                    "locale": "en"
                  },
                """);
    String arabic =
        """
        {
          "data": [
            {
              "id": 2, "documentId": "veh", "title": "تسجيل مركبة",
              "summary": "سجّل مركبة.", "instructions": null, "whatYouNeed": null,
              "expectedDuration": null, "processDefinitionId": "vehicle-registration",
              "locale": "ar"
            }
          ],
          "meta": { "pagination": { "page": 1, "pageSize": 100, "pageCount": 1, "total": 1 } }
        }
        """;

    RestClient.Builder builder = RestClient.builder().baseUrl("http://strapi.test");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo(Matchers.not(Matchers.containsString("locale="))))
        .andRespond(withSuccess(english, MediaType.APPLICATION_JSON));
    server
        .expect(requestTo(Matchers.containsString("locale=ar")))
        .andRespond(withSuccess(arabic, MediaType.APPLICATION_JSON));

    var services = new StrapiClient(builder.build()).fetchServicesById("ar");

    assertThat(services).containsOnlyKeys("vehicle-registration", "business-registration");
    assertThat(services.get("vehicle-registration").title()).isEqualTo("تسجيل مركبة");
    assertThat(services.get("business-registration").title()).isEqualTo("Business registration");
    server.verify();
  }

  @Test
  void unknownLocaleBehavesAsEnglish() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://strapi.test");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo(Matchers.not(Matchers.containsString("locale="))))
        .andRespond(withSuccess(V5_RESPONSE, MediaType.APPLICATION_JSON));

    var services = new StrapiClient(builder.build()).fetchServicesById("de");

    assertThat(services).containsOnlyKeys("vehicle-registration");
    server.verify(); // exactly one request — no locale fetch for unsupported languages
  }

  @Test
  void fetchesFormStringsForArabic() {
    String response =
        """
        {
          "data": [
            {
              "id": 1, "documentId": "abc", "formId": "vehicle-registration-start",
              "strings": { "ownerName": { "label": "اسم المالك" } }, "locale": "ar"
            }
          ]
        }
        """;
    RestClient.Builder builder = RestClient.builder().baseUrl("http://strapi.test");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo(Matchers.containsString("/api/form-translations")))
        .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

    var strings =
        new StrapiClient(builder.build()).fetchFormStrings("vehicle-registration-start", "ar");

    assertThat(strings).isNotNull();
    assertThat(strings.at("/ownerName/label").stringValue()).isEqualTo("اسم المالك");
  }

  @Test
  void formStringsAreNullForEnglishWithoutAnyRequest() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://strapi.test");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    // no expectations: any request would fail the test

    assertThat(new StrapiClient(builder.build()).fetchFormStrings("any-form", null)).isNull();
    assertThat(new StrapiClient(builder.build()).fetchFormStrings("any-form", "en")).isNull();
    server.verify();
  }

  @Test
  void formStringsDegradeToNullWhenStrapiFails() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://strapi.test");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo(Matchers.containsString("/api/form-translations")))
        .andRespond(withServerError());

    assertThat(new StrapiClient(builder.build()).fetchFormStrings("vehicle-registration-start", "ar"))
        .isNull();
  }
}
