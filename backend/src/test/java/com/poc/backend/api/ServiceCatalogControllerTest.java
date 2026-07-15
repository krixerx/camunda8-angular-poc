package com.poc.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.poc.backend.strapi.StrapiService;
import io.camunda.client.api.search.response.ProcessDefinition;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Covers the merge scenarios of the service-catalog spec without a running engine or CMS. */
class ServiceCatalogControllerTest {

  private static final StrapiService VEHICLE_CONTENT =
      new StrapiService(
          "Vehicle registration",
          "Register a vehicle.",
          "Fill in the form.",
          "- VIN",
          "1-2 working days",
          "vehicle-registration");

  private static ProcessDefinition definition(String id, String name) {
    ProcessDefinition pd = mock(ProcessDefinition.class);
    when(pd.getProcessDefinitionKey()).thenReturn(42L);
    when(pd.getProcessDefinitionId()).thenReturn(id);
    when(pd.getName()).thenReturn(name);
    when(pd.getVersion()).thenReturn(3);
    return pd;
  }

  @Test
  void mergedItemCarriesEngineAndContentFields() {
    var items =
        ServiceCatalogController.merge(
            List.of(definition("vehicle-registration", "Vehicle registration process")),
            Map.of("vehicle-registration", VEHICLE_CONTENT));

    assertThat(items).hasSize(1);
    var item = items.getFirst();
    assertThat(item.processDefinitionKey()).isEqualTo(42L);
    assertThat(item.processDefinitionId()).isEqualTo("vehicle-registration");
    assertThat(item.name()).isEqualTo("Vehicle registration process");
    assertThat(item.version()).isEqualTo(3);
    assertThat(item.title()).isEqualTo("Vehicle registration");
    assertThat(item.summary()).isEqualTo("Register a vehicle.");
    assertThat(item.instructions()).isEqualTo("Fill in the form.");
    assertThat(item.whatYouNeed()).isEqualTo("- VIN");
    assertThat(item.expectedDuration()).isEqualTo("1-2 working days");
  }

  @Test
  void definitionWithoutContentYieldsEngineOnlyItem() {
    var items =
        ServiceCatalogController.merge(
            List.of(definition("business-registration", "Business registration")), Map.of());

    assertThat(items).hasSize(1);
    var item = items.getFirst();
    assertThat(item.processDefinitionId()).isEqualTo("business-registration");
    assertThat(item.name()).isEqualTo("Business registration");
    assertThat(item.title()).isNull();
    assertThat(item.summary()).isNull();
    assertThat(item.instructions()).isNull();
    assertThat(item.whatYouNeed()).isNull();
    assertThat(item.expectedDuration()).isNull();
  }

  @Test
  void engineNameFallsBackToProcessDefinitionId() {
    var items =
        ServiceCatalogController.merge(
            List.of(definition("vehicle-registration", null)), Map.of());

    assertThat(items.getFirst().name()).isEqualTo("vehicle-registration");
  }

  @Test
  void orphanedCmsEntryProducesNoItem() {
    var items =
        ServiceCatalogController.merge(
            List.of(definition("business-registration", "Business registration")),
            Map.of("vehicle-registration", VEHICLE_CONTENT));

    assertThat(items).hasSize(1);
    assertThat(items.getFirst().processDefinitionId()).isEqualTo("business-registration");
  }
}
