package com.symphony;

import com.symphony.generated.api.CoffeeMakerApiV1Api;
import com.symphony.generated.api.CoffeeMakerApiV2Api;
import com.symphony.generated.model.CoffeeMakerStatus;
import com.symphony.generated.model.CoffeeSpecsV1;
import com.symphony.generated.model.CoffeeSpecsV2;
import com.symphony.generated.model.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.symphony.generated.model.CoffeeMakerStatus.StateEnum.HEATING;
import static com.symphony.generated.model.OrderResponse.StatusEnum.QUEUED;
import static java.util.Optional.ofNullable;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CoffeeControllerV2 implements CoffeeMakerApiV2Api {

  private final CoffeeStore store;

  @Override
  public ResponseEntity<OrderResponse> orderCoffee(@Valid CoffeeSpecsV2 spec) {
    log.info("Incoming coffee request: {}", spec);
    return ResponseEntity.ok(new OrderResponse()
      .orderId(store.enqueueOrder(spec))
      .status(QUEUED)
      .spec(spec));
  }
}
