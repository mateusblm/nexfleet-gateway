package br.com.nexfleet.gateway.observability;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class CorrelationIdWebFilter implements WebFilter {
  public static final String HEADER = "X-Correlation-ID";
  private static final String MDC_KEY = "correlationId";
  private static final Logger LOG = LoggerFactory.getLogger(CorrelationIdWebFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String correlationId = resolve(exchange.getRequest().getHeaders().getFirst(HEADER));
    long inicio = System.nanoTime();
    var requisicao = exchange.getRequest().mutate()
        .headers(headers -> headers.set(HEADER, correlationId))
        .build();
    var atualizado = exchange.mutate().request(requisicao).build();
    atualizado.getResponse().beforeCommit(() -> {
      atualizado.getResponse().getHeaders().set(HEADER, correlationId);
      return Mono.empty();
    });

    return chain.filter(atualizado).doFinally(sinal -> {
      if (!atualizado.getRequest().getPath().value().startsWith("/api/")) return;
      int status = atualizado.getResponse().getStatusCode() == null
          ? HttpStatus.OK.value()
          : atualizado.getResponse().getStatusCode().value();
      long duracaoMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
      try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_KEY, correlationId)) {
        LOG.info("http_request_completed method={} path={} status={} durationMs={}",
            atualizado.getRequest().getMethod(), atualizado.getRequest().getPath().value(),
            status, duracaoMs);
      }
    });
  }

  static String resolve(String candidate) {
    if (candidate != null && candidate.length() == 36) {
      try {
        return UUID.fromString(candidate).toString();
      } catch (IllegalArgumentException ignored) {
        // Um cabeçalho inválido nunca é copiado para logs nem para serviços internos.
      }
    }
    return UUID.randomUUID().toString();
  }
}
