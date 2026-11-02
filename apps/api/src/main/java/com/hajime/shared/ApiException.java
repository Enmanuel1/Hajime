package com.hajime.shared;

public class ApiException extends RuntimeException {
  private final int status;
  private final String code;

  public ApiException(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }

  public static ApiException denied() {
    return new ApiException(
        403, "ACCESS_DENIED", "Access is not permitted in this organization or for this record.");
  }

  public static ApiException missing(String code) {
    return new ApiException(404, code, "The requested record was not found.");
  }
}
