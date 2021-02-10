package com.symphony;

import com.symphony.generated.model.CoffeeSpecs;
import com.symphony.generated.model.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.symphony.generated.model.OrderResponse.StatusEnum.QUEUED;
import static java.util.Optional.ofNullable;

@Slf4j
@Service
public class CoffeeStore {

  private Map<UUID, CoffeeSpecs> orderSpecs = new ConcurrentHashMap<>();
  private Map<UUID, OrderResponse.StatusEnum> orderStatuses = new ConcurrentHashMap<>();

  public UUID enqueueOrder(CoffeeSpecs spec) {
    UUID id = UUID.randomUUID();
    orderSpecs.put(id, spec);
    orderStatuses.put(id, QUEUED);
    return id;
  }

  public Optional<Pair<CoffeeSpecs, OrderResponse.StatusEnum>> getOrder(UUID id) {
    return ofNullable(orderSpecs.get(id))
      .map(spec -> Pair.of(spec, orderStatuses.get(id)));
  }

  public void cancelOrder(UUID id) {
    orderSpecs.remove(id);
    orderStatuses.remove(id);
  }
}
