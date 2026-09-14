package org.insurance.registryemulator.unit.emulator.controller;

import static org.insurance.registryemulator.unit.emulator.EmulatorTestConstants.INVALID_MODE_NAME;
import static org.insurance.registryemulator.unit.emulator.EmulatorTestConstants.JSON_PATH_MODE;
import static org.insurance.registryemulator.unit.emulator.EmulatorTestConstants.MODE_ENDPOINT_URL;
import static org.insurance.registryemulator.unit.emulator.EmulatorTestConstants.MODE_SUCCESS_NAME;
import static org.insurance.registryemulator.unit.emulator.EmulatorTestConstants.MODE_TECHNICAL_ERROR_NAME;
import static org.insurance.registryemulator.unit.emulator.EmulatorTestConstants.PARAM_TYPE;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.insurance.registryemulator.emulator.controller.EmulatorModeController;
import org.insurance.registryemulator.emulator.domain.EmulatorMode;
import org.insurance.registryemulator.emulator.service.EmulatorModeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmulatorModeController.class)
class EmulatorModeControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private EmulatorModeService modeService;

  @Test
  void returnsCurrentMode() throws Exception {
    given(modeService.currentMode()).willReturn(EmulatorMode.SUCCESS);

    mockMvc
        .perform(get(MODE_ENDPOINT_URL))
        .andExpect(status().isOk())
        .andExpect(jsonPath(JSON_PATH_MODE).value(MODE_SUCCESS_NAME));
  }

  @Test
  void switchesMode() throws Exception {
    given(modeService.switchMode(EmulatorMode.TECHNICAL_ERROR))
        .willReturn(EmulatorMode.TECHNICAL_ERROR);

    mockMvc
        .perform(put(MODE_ENDPOINT_URL).param(PARAM_TYPE, MODE_TECHNICAL_ERROR_NAME))
        .andExpect(status().isOk())
        .andExpect(jsonPath(JSON_PATH_MODE).value(MODE_TECHNICAL_ERROR_NAME));
  }

  @Test
  void rejectsUnknownMode() throws Exception {
    mockMvc
        .perform(put(MODE_ENDPOINT_URL).param(PARAM_TYPE, INVALID_MODE_NAME))
        .andExpect(status().isBadRequest());
  }
}
