package com.example.marketplace.common.pagination;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paged API response.")
public record PageResponse<T>(
    @Schema(description = "Page content.")
    List<T> content,
    @Schema(description = "Zero-based page index.", example = "0")
    int page,
    @Schema(description = "Requested page size.", example = "20")
    int size,
    @Schema(description = "Total number of matching elements.", example = "42")
    long totalElements,
    @Schema(description = "Total number of pages.", example = "3")
    int totalPages,
    @Schema(description = "Whether this is the last page.", example = "false")
    boolean last
) {

  public static <T> PageResponse<T> of(List<T> content, PageRequest request, long totalElements) {
    int totalPages = totalElements == 0
        ? 0
        : (int) Math.ceil((double) totalElements / request.size());
    boolean last = totalPages == 0 || request.page() >= totalPages - 1;

    return new PageResponse<>(
        List.copyOf(content),
        request.page(),
        request.size(),
        totalElements,
        totalPages,
        last
    );
  }
}
