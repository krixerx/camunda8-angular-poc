package com.poc.backend.api;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ProblemException;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Process catalog and process instance endpoints backed by the Orchestration Cluster API v2. */
@RestController
@RequestMapping("/api")
public class ProcessController {

  private final CamundaClient client;
  private final ObjectMapper objectMapper;

  public ProcessController(CamundaClient client, ObjectMapper objectMapper) {
    this.client = client;
    this.objectMapper = objectMapper;
  }

  public record ProcessDefinitionDto(
      long processDefinitionKey, String processDefinitionId, String name, int version) {}

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
        .map(
            pd ->
                new ProcessDefinitionDto(
                    pd.getProcessDefinitionKey(),
                    pd.getProcessDefinitionId(),
                    pd.getName() != null ? pd.getName() : pd.getProcessDefinitionId(),
                    pd.getVersion()))
        .toList();
  }

  @GetMapping("/process-definitions/{processDefinitionKey}/form")
  public ResponseEntity<JsonNode> startForm(@PathVariable long processDefinitionKey) {
    try {
      var form = client.newProcessDefinitionGetFormRequest(processDefinitionKey).send().join();
      if (form == null || form.getSchema() == null) {
        return ResponseEntity.noContent().build();
      }
      return ResponseEntity.ok(objectMapper.readTree(form.getSchema()));
    } catch (ProblemException e) {
      if (e.details() != null && e.details().getStatus() == 404) {
        return ResponseEntity.noContent().build();
      }
      throw e;
    }
  }

  public record StartInstanceRequest(Map<String, Object> variables) {}

  public record StartInstanceResponse(long processInstanceKey) {}

  @PostMapping("/process-definitions/{processDefinitionId}/start")
  public StartInstanceResponse start(
      @PathVariable String processDefinitionId, @RequestBody(required = false) StartInstanceRequest request) {
    var command =
        client.newCreateInstanceCommand().bpmnProcessId(processDefinitionId).latestVersion();
    if (request != null && request.variables() != null && !request.variables().isEmpty()) {
      command = command.variables(request.variables());
    }
    var event = command.send().join();
    return new StartInstanceResponse(event.getProcessInstanceKey());
  }

  public record ProcessInstanceDto(
      long processInstanceKey,
      String processDefinitionId,
      String processDefinitionName,
      String state,
      String startDate,
      String endDate) {}

  @GetMapping("/process-instances")
  public List<ProcessInstanceDto> processInstances() {
    return client
        .newProcessInstanceSearchRequest()
        .sort(s -> s.startDate().desc())
        .send()
        .join()
        .items()
        .stream()
        .map(
            pi ->
                new ProcessInstanceDto(
                    pi.getProcessInstanceKey(),
                    pi.getProcessDefinitionId(),
                    pi.getProcessDefinitionName(),
                    pi.getState() != null ? pi.getState().name() : null,
                    pi.getStartDate() != null ? pi.getStartDate().toString() : null,
                    pi.getEndDate() != null ? pi.getEndDate().toString() : null))
        .toList();
  }
}
