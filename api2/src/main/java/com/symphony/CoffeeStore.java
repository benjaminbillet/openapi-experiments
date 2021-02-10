package com.symphony;

import com.symphony.generated.api.CoffeeMakerApiV1Api;
import com.symphony.generated.api.CoffeeMakerApiV2Api;
import com.symphony.generated.model.CoffeeMakerStatus;
import com.symphony.generated.model.CoffeeSpecsV1;
import com.symphony.generated.model.CoffeeSpecsV2;
import com.symphony.generated.model.OrderResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.symphony.generated.model.CoffeeMakerStatus.StateEnum.HEATING;
import static com.symphony.generated.model.OrderResponse.StatusEnum.QUEUED;
import static java.util.Optional.ofNullable;

@Slf4j
@Service
public class CoffeeStore {

  private Map<UUID, CoffeeSpecsV2> orderSpecs = new ConcurrentHashMap<>();
  private Map<UUID, OrderResponse.StatusEnum> orderStatuses = new ConcurrentHashMap<>();

  public UUID enqueueOrder(CoffeeSpecsV2 spec) {
    UUID id = UUID.randomUUID();
    orderSpecs.put(id, spec);
    orderStatuses.put(id, QUEUED);
    return id;
  }

  public Optional<Pair<CoffeeSpecsV2, OrderResponse.StatusEnum>> getOrder(UUID id) {
    return ofNullable(orderSpecs.get(id))
      .map(spec -> Pair.of(spec, orderStatuses.get(id)));
  }

  public void cancelOrder(UUID id) {
    orderSpecs.remove(id);
    orderStatuses.remove(id);
  }
}
