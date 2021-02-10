package com.symphony;

import com.symphony.generated.api.CoffeeMakerApiApi;
import com.symphony.generated.model.CoffeeMakerStatus;
import com.symphony.generated.model.CoffeeSpecs;
import com.symphony.generated.model.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

import static com.symphony.generated.model.CoffeeMakerStatus.StateEnum.HEATING;
import static com.symphony.generated.model.OrderResponse.StatusEnum.QUEUED;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CoffeeController implements CoffeeMakerApiApi {

  private final CoffeeStore store;

  @Override
  public ResponseEntity<CoffeeMakerStatus> getCoffeeMakerStatus() {
    return ResponseEntity.ok(new CoffeeMakerStatus().state(HEATING));
  }

  @Override
  public ResponseEntity<OrderResponse> orderCoffee(@Valid CoffeeSpecs spec) {
    log.info("Incoming coffee request: {}", spec);
    return ResponseEntity.ok(new OrderResponse()
      .orderId(store.enqueueOrder(spec))
      .status(QUEUED)
      .spec(spec));
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
