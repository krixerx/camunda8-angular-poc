package com.poc.backend.api;

import com.poc.backend.api.dto.ProcessDefinitionDto;
import com.poc.backend.api.dto.ProcessInstanceDto;
import com.poc.backend.api.dto.StartInstanceRequest;
import com.poc.backend.api.dto.StartInstanceResponse;
import com.poc.backend.strapi.FormTranslator;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ProblemException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Process catalog and process instance endpoints backed by the Orchestration Cluster API v2. */
@RestController
@RequestMapping("/api")
public class ProcessController {

  private final CamundaClient client;
  private final ObjectMapper objectMapper;
  private final FormTranslator formTranslator;

  public ProcessController(
      CamundaClient client, ObjectMapper objectMapper, FormTranslator formTranslator) {
    this.client = client;
    this.objectMapper = objectMapper;
    this.formTranslator = formTranslator;
  }

  @GetMapping("/process-definitions")
  public List<ProcessDefinitionDto> processDefinitions() {
    return client
        .newProcessDefinitionSearchRequest()
        .filter(f -> f.isLatestVersion(true))
        // with isLatestVersion the API only allows sorting by processDefinitionId/tenantId
        .sort(s -> s.processDefinitionId().asc())
        .send()
        .join()
        .items()
        .stream()
        .map(ProcessDefinitionDto::from)
        .toList();
  }

  @GetMapping("/process-definitions/{processDefinitionKey}/form")
  public ResponseEntity<JsonNode> startForm(
      @PathVariable long processDefinitionKey, @RequestParam(required = false) String locale) {
    try {
      var form = client.newProcessDefinitionGetFormRequest(processDefinitionKey).send().join();
      if (form == null || form.getSchema() == null) {
        return ResponseEntity.noContent().build();
      }
      return ResponseEntity.ok(
          formTranslator.translate(objectMapper.readTree(form.getSchema()), locale));
    } catch (ProblemException e) {
      if (e.details() != null && e.details().getStatus() == 404) {
        return ResponseEntity.noContent().build();
      }
      throw e;
    }
  }

  @PostMapping("/process-definitions/{processDefinitionId}/start")
  public StartInstanceResponse start(
      @PathVariable String processDefinitionId,
      @RequestBody(required = false) StartInstanceRequest request) {
    var command =
        client.newCreateInstanceCommand().bpmnProcessId(processDefinitionId).latestVersion();
    if (request != null && request.variables() != null && !request.variables().isEmpty()) {
      command = command.variables(request.variables());
    }
    var event = command.send().join();
    return new StartInstanceResponse(event.getProcessInstanceKey());
  }

  @GetMapping("/process-instances")
  public List<ProcessInstanceDto> processInstances() {
    return client
        .newProcessInstanceSearchRequest()
        .sort(s -> s.startDate().desc())
        .send()
        .join()
        .items()
        .stream()
        .map(ProcessInstanceDto::from)
        .toList();
  }
}
