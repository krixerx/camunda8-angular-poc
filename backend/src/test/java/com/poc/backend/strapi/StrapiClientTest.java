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
}
