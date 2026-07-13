package com.poc.backend.api.dto;

import io.camunda.client.api.search.response.ProcessDefinition;

public record ProcessDefinitionDto(
    long processDefinitionKey, String processDefinitionId, String name, int version) {

  public static ProcessDefinitionDto from(ProcessDefinition pd) {
    return new ProcessDefinitionDto(
        pd.getProcessDefinitionKey(),
        pd.getProcessDefinitionId(),
        pd.getName() != null ? pd.getName() : pd.getProcessDefinitionId(),
        pd.getVersion());
  }
}
