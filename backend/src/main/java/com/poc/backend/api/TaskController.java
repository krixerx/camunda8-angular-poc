package com.poc.backend.api;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ProblemException;
import io.camunda.client.api.search.enums.UserTaskState;
import io.camunda.client.api.search.response.UserTask;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** User task endpoints backed by the Orchestration Cluster API v2. */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

  private final CamundaClient client;
  private final ObjectMapper objectMapper;

  public TaskController(CamundaClient client, ObjectMapper objectMapper) {
    this.client = client;
    this.objectMapper = objectMapper;
  }

  public record TaskDto(
      long userTaskKey,
      String name,
      String elementId,
      String processDefinitionId,
      String processName,
      long processInstanceKey,
      String creationDate,
      String state) {}

  @GetMapping
  public List<TaskDto> openTasks() {
    return client
        .newUserTaskSearchRequest()
        .filter(f -> f.state(UserTaskState.CREATED))
        .sort(s -> s.creationDate().desc())
        .send()
        .join()
        .items()
        .stream()
        .map(TaskController::toDto)
        .toList();
  }

  public record TaskDetailDto(TaskDto task, JsonNode formSchema, Map<String, Object> variables) {}

  @GetMapping("/{userTaskKey}")
  public TaskDetailDto taskDetail(@PathVariable long userTaskKey) {
    var task = client.newUserTaskGetRequest(userTaskKey).send().join();
    return new TaskDetailDto(toDto(task), formSchema(userTaskKey), variables(userTaskKey));
  }

  public record CompleteTaskRequest(Map<String, Object> variables) {}

  @PostMapping("/{userTaskKey}/complete")
  public void complete(
      @PathVariable long userTaskKey, @RequestBody(required = false) CompleteTaskRequest request) {
    var command = client.newCompleteUserTaskCommand(userTaskKey);
    if (request != null && request.variables() != null && !request.variables().isEmpty()) {
      command = command.variables(request.variables());
    }
    command.send().join();
  }

  private static TaskDto toDto(UserTask task) {
    return new TaskDto(
        task.getUserTaskKey(),
        task.getName(),
        task.getElementId(),
        task.getBpmnProcessId(),
        task.getProcessName(),
        task.getProcessInstanceKey(),
        task.getCreationDate() != null ? task.getCreationDate().toString() : null,
        task.getState() != null ? task.getState().name() : null);
  }

  private JsonNode formSchema(long userTaskKey) {
    try {
      var form = client.newUserTaskGetFormRequest(userTaskKey).send().join();
      if (form == null || form.getSchema() == null) {
        return null;
      }
      return objectMapper.readTree(form.getSchema());
    } catch (ProblemException e) {
      if (e.details() != null && e.details().getStatus() == 404) {
        return null;
      }
      throw e;
    }
  }

  /** Effective (local + global) variables visible to the task, parsed from their JSON values. */
  private Map<String, Object> variables(long userTaskKey) {
    Map<String, Object> variables = new HashMap<>();
    client
        .newUserTaskEffectiveVariableSearchRequest(userTaskKey)
        .send()
        .join()
        .items()
        .forEach(v -> variables.put(v.getName(), parseValue(v.getValue())));
    return variables;
  }

  private Object parseValue(String json) {
    if (json == null) {
      return null;
    }
    return objectMapper.readValue(json, Object.class);
  }
}
