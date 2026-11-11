# NexFleet — API Gateway

Ponto de entrada HTTP do back-end do NexFleet, plataforma de gestão de frotas. Parte da v1.0.0; o sistema completo está no repositório guarda-chuva [nexfleet](https://github.com/mateusblm/nexfleet).

## Arquitetura

Na nuvem, o APIM encaminha para este Gateway; localmente ele é a porta de entrada (`127.0.0.1:8080`). O Gateway roteia as rotas `/api/v1/...` e `/aggregated-data` para o BFF e publica a **Swagger UI única** em `/swagger-ui.html`, com seletor entre BFF, Frota, Operações, Identidade e Indicadores (rotas `/docs/<svc>/v3/api-docs` encaminhadas a cada serviço).

```text
Navegador → shell + microfrontends (Static Web Apps)
          → Azure API Management (APIM) → Gateway → BFF
              ├─ MS1 Frota ........ MongoDB Atlas
              ├─ MS2 Operações .... Azure SQL (schema operacoes)
              ├─ Identidade, Indicadores ... Azure SQL (um schema e um login por serviço)
              └─ Azure Function HTTP calcular-eficiencia
Operações → outbox → Service Bus (nuvem) / RabbitMQ (local) → Indicadores + Function (notificação)
```

**Organização.** O Gateway não tem regra de negócio nem casos de uso, então não há fatias verticais; o código fica em `br.com.nexfleet.gateway`:

| Pacote | Responsabilidade |
|---|---|
| `config` | tabela de rotas (`GatewayConfig`) para o BFF e para os OpenAPI dos serviços |
| `observability` | filtro que gera ou propaga o `X-Correlation-ID` |

Os demais serviços seguem Clean Architecture + Vertical Slice; aqui o ArchUnit garante que o Gateway não importa código interno de outros serviços.

**Testes:** de rota e de Swagger (`GatewayHttpTest`, `SwaggerUiHttpTest`) e de arquitetura com ArchUnit (`ArchitectureTest`).

## Tecnologias

- Java 21, Spring Boot 4.1.1, Spring Cloud 2025.1.2 (Spring Cloud Gateway Server WebFlux)
- Spring Security, Actuator, Micrometer + Prometheus
- springdoc-openapi 3.2.0 (Swagger UI WebFlux)
- JUnit 5, ArchUnit 1.4.1
- Maven Wrapper (Maven 3.9.16)
- Docker (`eclipse-temurin:21`), GitHub Actions (testes em todo push; imagem no Docker Hub em tag `v*`)

## Como rodar localmente

```bash
./mvnw -B -ntp verify                                  # testes unitários e de arquitetura
docker build -t mateusblm/nexfleet-gateway:local .     # imagem (porta 8080)
```

Variáveis de ambiente: `PORT`, `BFF_URL`, `IDENTIDADE_URL`, `FROTA_URL`, `OPERACOES_URL`, `INDICADORES_URL`.

**Ambiente completo** (todos os serviços, SQL Server, MongoDB e RabbitMQ locais) sobe pelo repositório guarda-chuva [nexfleet](https://github.com/mateusblm/nexfleet), com os repositórios clonados lado a lado (`scripts/clonar-tudo.sh`):

```bash
scripts/iniciar-local.sh                                     # 1ª vez: gera o .env e a chave RSA local
docker compose -f compose.yaml -f compose.build.yaml up -d --build
scripts/seed-demo.sh                                         # dados de demonstração
```

Sem o override `compose.build.yaml`, o `docker compose up -d` usa as imagens publicadas no Docker Hub.

## URLs na nuvem

O Gateway não tem URL pública direta: na nuvem ele fica atrás do APIM, que expõe as rotas abaixo.

Imagem Docker: [`mateusblm/nexfleet-gateway:v1.0.0`](https://hub.docker.com/r/mateusblm/nexfleet-gateway)

| Recurso | Endereço |
|---|---|
| Frontend | https://white-mushroom-0cadda010.3.azurestaticapps.net |
| API (APIM) | https://apim-nexfleet-homolog-ukzjodf42myfe.azure-api.net |
| Swagger UI (BFF, Frota, Operações, Identidade, Indicadores) | https://apim-nexfleet-homolog-ukzjodf42myfe.azure-api.net/swagger-ui.html |
| Dados agregados (exige token) | https://apim-nexfleet-homolog-ukzjodf42myfe.azure-api.net/aggregated-data |
| AsyncAPI dos eventos | https://white-mushroom-0cadda010.3.azurestaticapps.net/docs/eventos/ |
| Repositório guarda-chuva | https://github.com/mateusblm/nexfleet |

> **Partida a frio:** os Container Apps escalam a zero e o Azure SQL pausa sem uso. A primeira chamada após um período de inatividade pode levar de 1 a 2 minutos (ou responder 503/504); tente de novo em seguida.

## Equipe

- Vinícius dos Reis Góes
- Mateus Burlamaqui
- Lucas Roussenq
- Erik Carvalho
