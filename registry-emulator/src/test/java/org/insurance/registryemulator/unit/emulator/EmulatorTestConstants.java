package org.insurance.registryemulator.unit.emulator;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class EmulatorTestConstants {

  public static final String MODE_ENDPOINT_URL = "/api/emulator/mode";
  public static final String PARAM_TYPE = "type";
  public static final String JSON_PATH_MODE = "$.mode";

  public static final String MODE_SUCCESS_NAME = "SUCCESS";
  public static final String MODE_TECHNICAL_ERROR_NAME = "TECHNICAL_ERROR";
  public static final String INVALID_MODE_NAME = "NOT_A_MODE";
}
