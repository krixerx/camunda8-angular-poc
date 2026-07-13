package com.poc.backend.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resolves the registration fee for the vehicle-registration process.
 * Hardcoded lookup keyed by vehicle category — stands in for an external
 * registry/pricing integration (see the service's business spec).
 */
@Component
public class VehiclePriceWorker {

  private static final Logger log = LoggerFactory.getLogger(VehiclePriceWorker.class);

  private static final Map<String, Integer> PRICES_EUR =
      Map.of(
          "car", 150,
          "motorcycle", 80,
          "truck", 250);

  private static final int DEFAULT_PRICE_EUR = 100;

  @JobWorker(type = "fetch-vehicle-price")
  public Map<String, Object> fetchVehiclePrice(@Variable String category) {
    int price = PRICES_EUR.getOrDefault(category, DEFAULT_PRICE_EUR);
    log.info("Resolved registration fee for category '{}': {} EUR", category, price);
    return Map.of("price", price);
  }
}
