package com.example.marketplace.common.pagination;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestTests {

  @Test
  void calculatesOffsetFromPageAndSize() {
    PageRequest request = PageRequest.of(2, 25);

    assertThat(request.limit()).isEqualTo(25);
    assertThat(request.offset()).isEqualTo(50);
  }

  @Test
  void rejectsInvalidPageValues() {
    assertThatThrownBy(() -> PageRequest.of(-1, 20))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Page index must be zero or greater");

    assertThatThrownBy(() -> PageRequest.of(0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Page size must be between 1 and 100");

    assertThatThrownBy(() -> PageRequest.of(0, 101))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Page size must be between 1 and 100");
  }

  @Test
  void createsPageResponseMetadata() {
    PageRequest request = PageRequest.of(1, 2);

    PageResponse<String> response = PageResponse.of(List.of("third", "fourth"), request, 5);

    assertThat(response.content()).containsExactly("third", "fourth");
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.size()).isEqualTo(2);
    assertThat(response.totalElements()).isEqualTo(5);
    assertThat(response.totalPages()).isEqualTo(3);
    assertThat(response.last()).isFalse();
  }
}
