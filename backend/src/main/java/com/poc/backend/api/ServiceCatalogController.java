package com.poc.backend.api;

import com.poc.backend.api.dto.ServiceCatalogItem;
import com.poc.backend.strapi.StrapiClient;
import com.poc.backend.strapi.StrapiService;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.response.ProcessDefinition;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Citizen-facing service catalog: latest deployed process definitions joined to Strapi editorial
 * content on {@code processDefinitionId}. The raw engine view stays on
 * {@code /api/process-definitions}; this endpoint is the merged, presentation-ready one.
 */
@RestController
@RequestMapping("/api")
public class ServiceCatalogController {

  private final CamundaClient client;
  private final StrapiClient strapiClient;

  public ServiceCatalogController(CamundaClient client, StrapiClient strapiClient) {
    this.client = client;
    this.strapiClient = strapiClient;
  }

  @GetMapping("/services")
  public List<ServiceCatalogItem> services() {
    var definitions =
        client
            .newProcessDefinitionSearchRequest()
            .filter(f -> f.isLatestVersion(true))
            // with isLatestVersion the API only allows sorting by processDefinitionId/tenantId
            .sort(s -> s.processDefinitionId().asc())
            .send()
            .join()
            .items();
    return merge(definitions, strapiClient.fetchServicesById());
  }

  /**
   * Deployed definitions drive the join: an orphaned CMS entry (no matching definition) yields no
   * item, a definition without content yields an engine-only item.
   */
  static List<ServiceCatalogItem> merge(
      List<ProcessDefinition> definitions, Map<String, StrapiService> content) {
    return definitions.stream()
        .map(pd -> ServiceCatalogItem.from(pd, content.get(pd.getProcessDefinitionId())))
        .toList();
  }
}
