package com.hajime.shared;

import java.util.List;

public record PageResult<T>(List<T> content, int page, int size, long totalElements) {
  public static int limit(int page, int size) {
    if (page < 0 || page > 100000 || size < 1 || size > 100)
      throw new ApiException(400, "INVALID_PAGE", "page must be 0..100000 and size 1..100.");
    return size;
  }
}
