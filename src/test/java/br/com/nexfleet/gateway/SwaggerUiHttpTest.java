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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SwaggerUiHttpTest {
  private static final HttpServer bff = servicoFalso("bff");
  private static final HttpServer frota = servicoFalso("frota");
  @LocalServerPort int port;

  /** Serviço falso que devolve o próprio nome e o caminho recebido. */
  private static HttpServer servicoFalso(String nome) {
    try {
      var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            var body =
                ("{\"servico\":\""
                        + nome
                        + "\",\"path\":\""
                        + exchange.getRequestURI().getPath()
                        + "\",\"auth\":\""
                        + exchange.getRequestHeaders().getFirst("Authorization")
                        + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
              output.write(body);
            }
          });
      server.start();
      return server;
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("bff.url", () -> "http://127.0.0.1:" + bff.getAddress().getPort());
    registry.add("frota.url", () -> "http://127.0.0.1:" + frota.getAddress().getPort());
  }

  @AfterAll
  static void stopUpstream() {
    bff.stop(0);
    frota.stop(0);
  }

  private HttpResponse<String> get(String path) throws Exception {
    var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).build();
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void swaggerUiRedirecionaComCaminhoRelativoEServeAPagina() throws Exception {
    var redirect = get("/swagger-ui.html");
    assertEquals(302, redirect.statusCode());
    String location = redirect.headers().firstValue("Location").orElseThrow();
    // Atrás do APIM o Location precisa ser relativo, sem o host interno do Gateway.
    assertTrue(location.startsWith("/swagger-ui/index.html"), location);

    var pagina = get("/swagger-ui/index.html");
    assertEquals(200, pagina.statusCode());
    assertTrue(pagina.body().contains("swagger-ui"));
  }

  @Test
  void configuracaoDaUiListaOsCincoServicos() throws Exception {
    var request =
        HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + "/v3/api-docs/swagger-config"))
            .build();
    var config = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, config.statusCode());
    // A Swagger UI lê o JSON com fetch().json(), que decodifica sempre como UTF-8.
    String corpo = new String(config.body(), StandardCharsets.UTF_8);
    for (String servico : new String[] {"bff", "frota", "operacoes", "identidade", "indicadores"})
      assertTrue(corpo.contains("\"url\":\"/docs/" + servico + "/v3/api-docs\""), corpo);
    for (String nome : new String[] {"BFF", "Frota", "Operações", "Identidade", "Indicadores"})
      assertTrue(corpo.contains("\"name\":\"" + nome + "\""), corpo);
  }

  @Test
  void rotaDocsReescreveParaOOpenApiDoServico() throws Exception {
    var bffDocs = get("/docs/bff/v3/api-docs");
    assertEquals(200, bffDocs.statusCode());
    assertEquals("{\"servico\":\"bff\",\"path\":\"/v3/api-docs\",\"auth\":\"null\"}", bffDocs.body());

    var frotaDocs = get("/docs/frota/v3/api-docs");
    assertEquals(200, frotaDocs.statusCode());
    assertTrue(frotaDocs.body().contains("\"servico\":\"frota\""), frotaDocs.body());
  }

  @Test
  void rotaDocsNaoAbreOutrosCaminhosDoServico() throws Exception {
    var outro = get("/docs/frota/api/v1/frota/veiculos");
    assertNotEquals(200, outro.statusCode());
    assertFalse(outro.body().contains("\"servico\""), outro.body());
  }

  @Test
  void operacaoInternaChamadaPelaUiSemTokenRecebe401() throws Exception {
    // O "Try it out" usa o servidor relativo "/": caminhos internos não são roteados.
    var resposta = get("/api/v1/frota/veiculos");
    assertEquals(401, resposta.statusCode());
    assertFalse(resposta.body().contains("\"servico\""), resposta.body());
  }
}
