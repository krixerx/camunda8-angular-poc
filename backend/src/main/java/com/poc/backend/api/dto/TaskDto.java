package com.poc.backend.api.dto;

import io.camunda.client.api.search.response.UserTask;

public record TaskDto(
    long userTaskKey,
    String name,
    String elementId,
    String processDefinitionId,
    String processName,
    long processInstanceKey,
    String creationDate,
    String state) {

  public static TaskDto from(UserTask task) {
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
}
