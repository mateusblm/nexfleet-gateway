package br.com.nexfleet.gateway;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayHttpTest {
  private static final HttpServer upstream = startUpstream();
  @LocalServerPort int port;

  private static HttpServer startUpstream() {
    try {
      var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/api/v1/ambiente",
          exchange -> {
            var body =
                "{\"application\":\"NexFleet\",\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add(
                "X-Upstream-Correlation-ID",
                exchange.getRequestHeaders().getFirst("X-Correlation-ID"));
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
              output.write(body);
            }
          });
      server.createContext(
          "/api/v1/auth/login",
          exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
              output.write(body);
            }
          });
      server.createContext(
          "/api/v1/auth/me",
          exchange -> {
            var body =
                String.valueOf(exchange.getRequestHeaders().getFirst("Authorization"))
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, body.length);
            try (var output = exchange.getResponseBody()) {
              output.write(body);
            }
          });
      server.start();
      server.createContext(
          "/api/v1/usuarios",
          exchange -> {
            String echo =
                exchange.getRequestMethod()
                    + "|"
                    + exchange.getRequestHeaders().getFirst("Authorization")
                    + "|"
                    + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = echo.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(403, body.length);
            try (var output = exchange.getResponseBody()) {
              output.write(body);
            }
          });
      server.createContext(
          "/api/v1/veiculos",
          exchange -> {
            String echo =
                exchange.getRequestMethod()
                    + "|"
                    + exchange.getRequestHeaders().getFirst("Authorization")
                    + "|"
                    + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = echo.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(409, body.length);
            try (var output = exchange.getResponseBody()) {
              output.write(body);
            }
          });
      server.createContext(
          "/api/v1/motoristas",
          exchange -> {
            String echo =
                exchange.getRequestMethod()
                    + "|"
                    + exchange.getRequestHeaders().getFirst("Authorization")
                    + "|"
                    + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = echo.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(409, body.length);
            try (var output = exchange.getResponseBody()) {
              output.write(body);
            }
          });
      server.createContext(
          "/api/v1/vinculos",
          exchange -> {
            String echo =
                exchange.getRequestMethod()
                    + "|"
                    + exchange.getRequestHeaders().getFirst("Authorization")
                    + "|"
                    + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = echo.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(409, body.length);
            try (var output = exchange.getResponseBody()) {
              output.write(body);
            }
          });
      server.createContext(
          "/api/v1/abastecimentos",
          exchange -> {
            String echo = exchange.getRequestMethod() + "|"
                + exchange.getRequestHeaders().getFirst("Authorization") + "|"
                + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = echo.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(422, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
          });
      server.createContext(
          "/api/v1/manutencoes",
          exchange -> {
            String echo = exchange.getRequestMethod() + "|"
                + exchange.getRequestHeaders().getFirst("Authorization") + "|"
                + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = echo.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(422, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
          });
      server.createContext(
          "/api/v1/dashboard",
          exchange -> {
            String echo =
                exchange.getRequestMethod()
                    + "|"
                    + exchange.getRequestHeaders().getFirst("Authorization");
            byte[] body = echo.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
              output.write(body);
            }
          });
      return server;
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("bff.url", () -> "http://127.0.0.1:" + upstream.getAddress().getPort());
  }

  @AfterAll
  static void stopUpstream() {
    upstream.stop(0);
  }

  @Test
  void encaminhaConsultaAoBff() throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/ambiente")).build();
    var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("NexFleet"));
  }

  @Test
  void preservaCorrelationIdValidoNoBffENaResposta() throws Exception {
    String correlationId = UUID.randomUUID().toString();
    var request = HttpRequest.newBuilder(
            URI.create("http://127.0.0.1:" + port + "/api/v1/ambiente"))
        .header("X-Correlation-ID", correlationId)
        .build();

    var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(correlationId, response.headers().firstValue("X-Correlation-ID").orElseThrow());
    assertEquals(1, response.headers().allValues("X-Correlation-ID").size());
    assertEquals(correlationId,
        response.headers().firstValue("X-Upstream-Correlation-ID").orElseThrow());
  }

  @Test
  void substituiCorrelationIdInvalidoPorUuidSeguro() throws Exception {
    var request = HttpRequest.newBuilder(
            URI.create("http://127.0.0.1:" + port + "/api/v1/ambiente"))
        .header("X-Correlation-ID", "valor-invalido-com-quebra-nao-copiado")
        .build();

    var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    String correlationId = response.headers().firstValue("X-Correlation-ID").orElseThrow();

    assertEquals(correlationId, UUID.fromString(correlationId).toString());
    assertEquals(correlationId,
        response.headers().firstValue("X-Upstream-Correlation-ID").orElseThrow());
  }

  @Test
  void naoExpoeApiInternaDeFrota() throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/frota/info"))
            .build();
    var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    assertTrue(response.statusCode() == 401 || response.statusCode() == 403);
  }

  @Test
  void encaminhaPostLoginSemBloqueioCsrfEConservaCorpo() throws Exception {
    var body = "{\"email\":\"teste@example.com\",\"senha\":\"somente-teste\"}";
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertEquals(body, response.body());
  }

  @Test
  void preservaBearerERecusaDoBff() throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/auth/me"))
            .header("Authorization", "Bearer teste-invalido")
            .build();
    var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(401, response.statusCode());
    assertEquals("Bearer teste-invalido", response.body());
  }

  @Test
  void roteiaGestaoDeUsuariosPreservandoMetodoCorpoERecusa() throws Exception {
    for (String method : new String[] {"GET", "POST", "PUT", "DELETE"}) {
      String path =
          "/api/v1/usuarios"
              + (method.equals("PUT") || method.equals("DELETE")
                  ? "/e2f6b588-00cf-45bc-927e-c4a08647dfde"
                  : "");
      var request =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
              .header("Authorization", "Bearer token-teste")
              .header("Content-Type", "application/json")
              .method(method, HttpRequest.BodyPublishers.ofString("{}"))
              .build();
      var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(403, response.statusCode());
      assertEquals(method + "|Bearer token-teste|{}", response.body());
    }
  }

  @Test
  void roteiaVeiculosPreservandoMetodoCorpoEBearer() throws Exception {
    for (String method : new String[] {"GET", "POST", "PUT", "DELETE"}) {
      String path =
          "/api/v1/veiculos"
              + (method.equals("PUT") || method.equals("DELETE")
                  ? "/b176f1b1-bc54-4f22-9b2e-fb69469ab033"
                  : "");
      var request =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
              .header("Authorization", "Bearer token-frota")
              .header("Content-Type", "application/json")
              .method(method, HttpRequest.BodyPublishers.ofString("{}"))
              .build();
      var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(409, response.statusCode());
      assertEquals(method + "|Bearer token-frota|{}", response.body());
    }
  }

  @Test
  void roteiaMotoristasPreservandoMetodoCorpoEBearer() throws Exception {
    for (String method : new String[] {"GET", "POST", "PUT"}) {
      String path =
          "/api/v1/motoristas"
              + (method.equals("PUT") ? "/3ce0bdb7-f1de-47c2-baa3-7b99e343650c" : "");
      var request =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
              .header("Authorization", "Bearer token-frota")
              .header("Content-Type", "application/json")
              .method(method, HttpRequest.BodyPublishers.ofString("{}"))
              .build();
      var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(409, response.statusCode());
      assertEquals(method + "|Bearer token-frota|{}", response.body());
    }
  }

  @Test
  void roteiaCriacaoConsultaEEncerramentoDeVinculos() throws Exception {
    for (String method : new String[] {"GET", "POST", "PUT"}) {
      String path =
          method.equals("PUT")
              ? "/api/v1/vinculos/1d4be0d1-4801-41e1-9c5f-9002b2fc6dcc/encerramento"
              : "/api/v1/vinculos";
      var request =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
              .header("Authorization", "Bearer token-frota")
              .header("Content-Type", "application/json")
              .method(method, HttpRequest.BodyPublishers.ofString("{}"))
              .build();
      var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(409, response.statusCode());
      assertEquals(method + "|Bearer token-frota|{}", response.body());
    }
  }

  @Test
  void roteiaConsultaECriacaoDeAbastecimentos() throws Exception {
    for (String method : new String[] {"GET", "POST"}) {
      var request = HttpRequest.newBuilder(
              URI.create("http://127.0.0.1:" + port + "/api/v1/abastecimentos"))
          .header("Authorization", "Bearer token-operacoes")
          .header("Content-Type", "application/json")
          .method(method, HttpRequest.BodyPublishers.ofString("{}"))
          .build();
      var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(422, response.statusCode());
      assertEquals(method + "|Bearer token-operacoes|{}", response.body());
    }
  }

  @Test
  void roteiaConsultaECriacaoDeManutencoes() throws Exception {
    for (String method : new String[] {"GET", "POST"}) {
      var request = HttpRequest.newBuilder(
              URI.create("http://127.0.0.1:" + port + "/api/v1/manutencoes"))
          .header("Authorization", "Bearer token-operacoes")
          .header("Content-Type", "application/json")
          .method(method, HttpRequest.BodyPublishers.ofString("{}"))
          .build();
      var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(422, response.statusCode());
      assertEquals(method + "|Bearer token-operacoes|{}", response.body());
    }
  }

  @Test
  void roteiaExclusaoDefinitivaSomenteComDelete() throws Exception {
    var esperado =
        java.util.Map.of(
            "/api/v1/veiculos/b176f1b1-bc54-4f22-9b2e-fb69469ab033/definitivo", 409,
            "/api/v1/motoristas/3ce0bdb7-f1de-47c2-baa3-7b99e343650c/definitivo", 409,
            "/api/v1/usuarios/e2f6b588-00cf-45bc-927e-c4a08647dfde/definitivo", 403);
    for (var item : esperado.entrySet()) {
      var request =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + item.getKey()))
              .header("Authorization", "Bearer token-exclusao")
              .DELETE()
              .build();
      var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(item.getValue(), response.statusCode(), item.getKey());
      assertTrue(response.body().startsWith("DELETE|Bearer token-exclusao"), item.getKey());

      // Outros métodos nessa rota não chegam ao BFF.
      var get =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + item.getKey()))
              .header("Authorization", "Bearer token-exclusao")
              .build();
      var negado = HttpClient.newHttpClient().send(get, HttpResponse.BodyHandlers.ofString());
      assertFalse(negado.body().startsWith("GET|"), item.getKey());
    }
  }

  @Test
  void roteiaDashboardPreservandoBearer() throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/dashboard"))
            .header("Authorization", "Bearer token-dashboard")
            .build();
    var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertEquals("GET|Bearer token-dashboard", response.body());
  }
}
