package com.poc.backend.api.dto;

import io.camunda.client.api.search.response.ProcessInstance;

public record ProcessInstanceDto(
    long processInstanceKey,
    String processDefinitionId,
    String processDefinitionName,
    String state,
    String startDate,
    String endDate) {

  public static ProcessInstanceDto from(ProcessInstance pi) {
    return new ProcessInstanceDto(
        pi.getProcessInstanceKey(),
        pi.getProcessDefinitionId(),
        pi.getProcessDefinitionName(),
        pi.getState() != null ? pi.getState().name() : null,
        pi.getStartDate() != null ? pi.getStartDate().toString() : null,
        pi.getEndDate() != null ? pi.getEndDate().toString() : null);
  }
}
