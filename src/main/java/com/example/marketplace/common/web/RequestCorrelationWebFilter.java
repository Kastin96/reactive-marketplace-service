package com.example.marketplace.common.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestCorrelationWebFilter implements WebFilter {

  public static final String REQUEST_ID_HEADER = "X-Request-Id";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String headerRequestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
    String requestId = StringUtils.hasText(headerRequestId)
        ? headerRequestId
        : UUID.randomUUID().toString();

    String method = exchange.getRequest().getMethod().name();
    String path = exchange.getRequest().getPath().value();

    exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
    log.info("Request started requestId={} method={} path={}", requestId, method, path);

    return chain.filter(exchange)
        .doFinally(signalType -> {
          HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
          log.info(
              "Request completed requestId={} method={} path={} status={}",
              requestId,
              method,
              path,
              statusCode == null ? "UNKNOWN" : String.valueOf(statusCode.value())
          );
        });
  }
}
