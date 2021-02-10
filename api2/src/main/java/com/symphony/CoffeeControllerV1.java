package com.symphony;

import com.symphony.generated.api.CoffeeMakerApiV1Api;
import com.symphony.generated.model.CoffeeMakerStatus;
import com.symphony.generated.model.CoffeeSpecsV1;
import com.symphony.generated.model.CoffeeSpecsV2;
import com.symphony.generated.model.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

import static com.symphony.generated.model.CoffeeMakerStatus.StateEnum.HEATING;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CoffeeControllerV1 implements CoffeeMakerApiV1Api {

  private final CoffeeStore store;
  private final CoffeeControllerV2 coffeeControllerV2;

  @Override
  public ResponseEntity<CoffeeMakerStatus> getCoffeeMakerStatus() {
    return ResponseEntity.ok(new CoffeeMakerStatus().state(HEATING));
  }

  @Override
  public ResponseEntity<OrderResponse> orderCoffee(@Valid CoffeeSpecsV1 spec) {
    return coffeeControllerV2.orderCoffee((CoffeeSpecsV2) new CoffeeSpecsV2()
      .coffeeType(spec.getCoffeeType())
      .strength(spec.getStrength()));
  }

  @Override
  public ResponseEntity<OrderResponse> getCoffeeOrder(UUID orderId) {
    return store.getOrder(orderId)
      .map(pair -> ResponseEntity.ok(new OrderResponse()
        .orderId(orderId)
        .spec(pair.getKey())
        .status(pair.getValue())))
      .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<Void> cancelCoffeeOrder(UUID orderId) {
    store.cancelOrder(orderId);
    return ResponseEntity.noContent().build();
  }
}
