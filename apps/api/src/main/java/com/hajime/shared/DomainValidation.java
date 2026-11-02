package com.hajime.shared;

/** Invariants shared by domain values, including callers outside HTTP. */
public final class DomainValidation {
  private DomainValidation() {}

  public static void require(boolean valid, String message) {
    if (!valid) throw new ApiException(400, "INVALID_RECORD", message);
  }

  public static <T> T required(T value, String name) {
    require(value != null, name + " is required.");
    return value;
  }

  public static String text(String value, int max, String name) {
    require(value != null && !value.isBlank(), name + " is required.");
    optionalText(value, max, name);
    return value.trim();
  }

  public static void optionalText(String value, int max, String name) {
    require(value == null || value.length() <= max, name + " is too long.");
  }

  public static void range(Integer value, int min, int max, String name) {
    require(value == null || value >= min && value <= max, name + " is out of range.");
  }
}
