package com.poc.backend.api.dto;

import java.util.Map;

public record StartInstanceRequest(Map<String, Object> variables) {}
