package com.hajime.shared;

/** Application/domain failure. Transport adapters decide how to represent its category. */
public final class BusinessException extends RuntimeException {
  public enum Kind {
    INVALID_INPUT,
    UNAUTHENTICATED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    UNSUPPORTED
  }

  private final Kind kind;
  private final String code;

  private BusinessException(Kind kind, String code, String message) {
    super(message);
    this.kind = kind;
    this.code = code;
  }

  public Kind kind() {
    return kind;
  }

  public String code() {
    return code;
  }

  public static BusinessException invalid(String code, String message) {
    return new BusinessException(Kind.INVALID_INPUT, code, message);
  }

  public static BusinessException unauthenticated(String code, String message) {
    return new BusinessException(Kind.UNAUTHENTICATED, code, message);
  }

  public static BusinessException forbidden(String code, String message) {
    return new BusinessException(Kind.FORBIDDEN, code, message);
  }

  public static BusinessException notFound(String code, String message) {
    return new BusinessException(Kind.NOT_FOUND, code, message);
  }

  public static BusinessException conflict(String code, String message) {
    return new BusinessException(Kind.CONFLICT, code, message);
  }

  public static BusinessException unsupported(String code, String message) {
    return new BusinessException(Kind.UNSUPPORTED, code, message);
  }

  public static BusinessException denied() {
    return forbidden(
        "ACCESS_DENIED", "Access is not permitted in this organization or for this record.");
  }

  public static BusinessException missing(String code) {
    return notFound(code, "The requested record was not found.");
  }
}
