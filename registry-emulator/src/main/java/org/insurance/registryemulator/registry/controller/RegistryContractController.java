package org.insurance.registryemulator.registry.controller;

import lombok.RequiredArgsConstructor;
import org.insurance.registryemulator.registry.dto.RegistrationResult;
import org.insurance.registryemulator.registry.dto.RegistryContractRequest;
import org.insurance.registryemulator.registry.dto.RegistryContractResponse;
import org.insurance.registryemulator.registry.service.RegistryContractService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/registry/contracts")
@RequiredArgsConstructor
public class RegistryContractController {

  private static final String REGISTERED_AT = "registeredAt";

  private final RegistryContractService service;

  @PostMapping
  public ResponseEntity<RegistryContractResponse> register(
      @Valid @RequestBody RegistryContractRequest request) {

    RegistrationResult result = service.register(request);
    var status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
    return ResponseEntity.status(status).body(result.response());
  }

  @GetMapping
  public Page<RegistryContractResponse> list(
      @PageableDefault(size = 20, sort = REGISTERED_AT, direction = Sort.Direction.DESC)
          Pageable pageable) {
    return service.list(pageable);
  }

  @GetMapping("/{externalContractId}")
  public RegistryContractResponse getOne(@PathVariable UUID externalContractId) {
    return service.getById(externalContractId);
  }
}
