package com.poc.backend;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Deployment(
    resources = {
      "classpath*:processes/**/*.bpmn",
      "classpath*:processes/**/*.dmn",
      "classpath*:processes/**/*.form"
    })
public class BackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }
}
