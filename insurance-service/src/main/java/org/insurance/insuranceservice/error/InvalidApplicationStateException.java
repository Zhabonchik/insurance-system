package org.insurance.insuranceservice.error;

public class InvalidApplicationStateException extends RuntimeException {

  public InvalidApplicationStateException(String message) {
    super(message);
  }
}
