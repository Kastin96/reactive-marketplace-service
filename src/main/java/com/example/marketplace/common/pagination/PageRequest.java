package com.example.marketplace.common.pagination;

public record PageRequest(
    int page,
    int size
) {

  public static final int DEFAULT_PAGE = 0;
  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;
  public static final String DEFAULT_PAGE_VALUE = "0";
  public static final String DEFAULT_SIZE_VALUE = "20";

  public PageRequest {
    if (page < 0) {
      throw new IllegalArgumentException("Page index must be zero or greater");
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new IllegalArgumentException("Page size must be between 1 and " + MAX_SIZE);
    }
  }

  public static PageRequest of(int page, int size) {
    return new PageRequest(page, size);
  }

  public int limit() {
    return size;
  }

  public long offset() {
    return (long) page * size;
  }
}
