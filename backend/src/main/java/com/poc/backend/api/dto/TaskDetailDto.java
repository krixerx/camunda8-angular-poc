package com.poc.backend.api.dto;

import java.util.Map;
import tools.jackson.databind.JsonNode;

public record TaskDetailDto(TaskDto task, JsonNode formSchema, Map<String, Object> variables) {}
