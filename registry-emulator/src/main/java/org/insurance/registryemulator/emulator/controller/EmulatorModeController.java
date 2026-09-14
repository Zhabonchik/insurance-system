package org.insurance.registryemulator.emulator.controller;

import lombok.RequiredArgsConstructor;
import org.insurance.registryemulator.emulator.domain.EmulatorMode;
import org.insurance.registryemulator.emulator.dto.EmulatorModeResponse;
import org.insurance.registryemulator.emulator.service.EmulatorConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/emulator")
public class EmulatorModeController {

  private final EmulatorConfigService service;

  @GetMapping("/mode")
  public EmulatorModeResponse getMode() {
    var currentMode = service.currentMode();
    return new EmulatorModeResponse(currentMode);
  }

  @PutMapping("/mode")
  public EmulatorModeResponse switchMode(@RequestParam("type") EmulatorMode type) {
    var newMode = service.switchMode(type);
    return new EmulatorModeResponse(newMode);
  }
}
