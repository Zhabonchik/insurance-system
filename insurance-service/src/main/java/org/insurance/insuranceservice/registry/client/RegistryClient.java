package org.insurance.insuranceservice.registry.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.insurance.insuranceservice.registry.config.RegistryEmulatorProperties;
import org.insurance.insuranceservice.registry.dto.RegistryRegistrationRequest;
import org.insurance.insuranceservice.registry.error.RegistryPermanentException;
import org.insurance.insuranceservice.registry.error.RegistryTransientException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegistryClient {

  private final RegistryEmulatorProperties registryEmulatorProperties;
  private final RestClient registryRestClient;

  public void register(RegistryRegistrationRequest request) {
    try {
      registryRestClient
          .post()
          .uri(registryEmulatorProperties.registerPath())
          .contentType(APPLICATION_JSON)
          .body(request)
          .retrieve()
          .toBodilessEntity();
    } catch (HttpClientErrorException ex) {
      int statusCode = ex.getStatusCode().value();
      if (RegistryErrorClassifier.isRetryableStatus(statusCode)) {
        throw new RegistryTransientException(
            "Registry returned retryable status %d".formatted(statusCode), ex);
      }
      throw new RegistryPermanentException(
          "Registry rejected the contract with status %d".formatted(statusCode), ex);
    } catch (HttpServerErrorException ex) {
      throw new RegistryTransientException(
          "Registry returned server error %d".formatted(ex.getStatusCode().value()), ex);
    } catch (ResourceAccessException ex) {
      throw new RegistryTransientException("Registry is unreachable", ex);
    }
  }
}
