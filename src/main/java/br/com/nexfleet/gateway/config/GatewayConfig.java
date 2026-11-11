package br.com.nexfleet.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class GatewayConfig {
  @Bean
  RouteLocator routes(RouteLocatorBuilder builder, @Value("${bff.url}") String bffUrl) {
    return builder
        .routes()
        .route(
            "bff-manutencoes",
            route -> route.path("/api/v1/manutencoes").and().method("GET", "POST").uri(bffUrl))
        .route(
            "bff-dashboard",
            route -> route.path("/api/v1/dashboard").and().method("GET").uri(bffUrl))
        .route(
            "bff-abastecimentos",
            route -> route.path("/api/v1/abastecimentos").and().method("GET", "POST").uri(bffUrl))
        .route(
            "bff-vinculos-lista",
            route -> route.path("/api/v1/vinculos").and().method("GET", "POST").uri(bffUrl))
        .route(
            "bff-vinculos-encerramento",
            route ->
                route.path("/api/v1/vinculos/*/encerramento").and().method("PUT").uri(bffUrl))
        .route(
            "bff-motoristas-lista",
            route -> route.path("/api/v1/motoristas").and().method("GET", "POST").uri(bffUrl))
        .route(
            "bff-motoristas-item",
            route -> route.path("/api/v1/motoristas/*").and().method("GET", "PUT").uri(bffUrl))
        .route(
            "bff-veiculos-lista",
            route -> route.path("/api/v1/veiculos").and().method("GET", "POST").uri(bffUrl))
        .route(
            "bff-veiculos-item",
            route ->
                route.path("/api/v1/veiculos/*").and().method("GET", "PUT", "DELETE").uri(bffUrl))
        .route(
            "bff-usuarios-lista",
            route -> route.path("/api/v1/usuarios").and().method("GET", "POST").uri(bffUrl))
        .route(
            "bff-usuarios-item",
            route ->
                route.path("/api/v1/usuarios/*").and().method("GET", "PUT", "DELETE").uri(bffUrl))
        .route(
            "bff-exclusao-definitiva",
            route ->
                route
                    .path(
                        "/api/v1/veiculos/*/definitivo",
                        "/api/v1/motoristas/*/definitivo",
                        "/api/v1/usuarios/*/definitivo")
                    .and()
                    .method("DELETE")
                    .uri(bffUrl))
        .route(
            "bff-login", route -> route.path("/api/v1/auth/login").and().method("POST").uri(bffUrl))
        .route("bff-me", route -> route.path("/api/v1/auth/me").and().method("GET").uri(bffUrl))
        .route(
            "bff-ambiente", route -> route.path("/api/v1/ambiente").and().method("GET").uri(bffUrl))
        .build();
  }

  @Bean
  SecurityWebFilterChain security(ServerHttpSecurity http) {
    return http.csrf(csrf -> csrf.disable())
        .authorizeExchange(
            auth ->
                auth.pathMatchers(
                        HttpMethod.GET,
                        "/actuator/health",
                        "/actuator/health/**",
                        "/actuator/prometheus",
                        "/api/v1/ambiente")
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, "/api/v1/auth/login")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/v1/auth/me")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/v1/usuarios", "/api/v1/usuarios/*")
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, "/api/v1/usuarios")
                    .permitAll()
                    .pathMatchers(HttpMethod.PUT, "/api/v1/usuarios/*")
                    .permitAll()
                    .pathMatchers(HttpMethod.DELETE, "/api/v1/usuarios/*")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/v1/veiculos", "/api/v1/veiculos/*")
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, "/api/v1/veiculos")
                    .permitAll()
                    .pathMatchers(HttpMethod.PUT, "/api/v1/veiculos/*")
                    .permitAll()
                    .pathMatchers(HttpMethod.DELETE, "/api/v1/veiculos/*")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/v1/motoristas", "/api/v1/motoristas/*")
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, "/api/v1/motoristas")
                    .permitAll()
                    .pathMatchers(HttpMethod.PUT, "/api/v1/motoristas/*")
                    .permitAll()
                    .pathMatchers(
                        HttpMethod.DELETE,
                        "/api/v1/veiculos/*/definitivo",
                        "/api/v1/motoristas/*/definitivo",
                        "/api/v1/usuarios/*/definitivo")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/v1/vinculos")
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, "/api/v1/vinculos")
                    .permitAll()
                    .pathMatchers(HttpMethod.PUT, "/api/v1/vinculos/*/encerramento")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/v1/abastecimentos")
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, "/api/v1/abastecimentos")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/v1/manutencoes")
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, "/api/v1/manutencoes")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/v1/dashboard")
                    .permitAll()
                    .anyExchange()
                    .denyAll())
        .build();
  }
}
