package org.insurance.insuranceservice.registry.client;

import java.util.Set;

/** Classifies an HTTP status as retryable (transient) or not (permanent). */
public final class RegistryErrorClassifier {

  private static final Set<Integer> RETRYABLE_CLIENT_STATUSES = Set.of(408, 429);

  private RegistryErrorClassifier() {}

  public static boolean isRetryableStatus(int statusCode) {
    return statusCode >= 500 || RETRYABLE_CLIENT_STATUSES.contains(statusCode);
  }
}
