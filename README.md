# ecom-microservice-app

This repository is an example e-commerce microservice application built with Spring Boot and Spring Cloud components. It demonstrates a small microservices ecosystem including a service registry (Eureka), a configuration server, API gateway (Spring Cloud Gateway), and several backend services (customer, inventory, billing). The project uses Java 17 and Spring Boot 3.5.6 with Spring Cloud 2025.0.0.

---

## Table of Contents

- Overview
- Architecture & Components
- Tech stack
- Project layout
- Configuration (config-repo and Config Server)
- Ports and default endpoints
- How to build
- How to run (order + commands for Windows cmd.exe)
- Example requests
- Troubleshooting & tips
- Next steps / possible improvements

---

## Overview

This multi-module project demonstrates how to build and compose a microservice ecosystem using Spring Cloud primitives:

- Discovery (Eureka) for service registration and discovery
- Centralized configuration via Spring Cloud Config Server
- API Gateway using Spring Cloud Gateway (reactive)
- Backend services exposing REST endpoints and using Spring Data REST / JPA + in-memory H2
- Actuator endpoints exposed for health/metrics

The repository includes a small local `config-repo` folder with sample configuration files (properties) for services. The `config-service` module is configured to use a Git URI by default; you can change it to use the local `config-repo` if you prefer.

---

## Architecture & Components

Overview

This section describes the high-level architecture and each component's responsibility, default ports, and key endpoints.

Diagram

```
                       ┌──────────────────────────┐
                       │   Config Server (9999)   │
                       │   (config-service)       │
                       └────────────┬─────────────┘
                                    │ (optional config import)
                                    v
    Client (curl/browser)           ┌───────────────────────┐
             │ HTTP                 │     API Gateway       │
             └─────────────────────>│  (gateway-service)    │
                                    │       port: 8888      │
                                    └─────────┬─────────────┘
                                              │ routes (via Eureka)
                                              v
                                      ┌─────────────────────┐
                                      │   Eureka Server     │
                                      │ (discovery-service) │
                                      │      port: 8761     │
                                      └───────┬───┬───┬─────┘
                                              │   │   │
                       ┌──────────────────────┘   │   └────────────┐
                       │                          │                │
                       v                          v                v
               ┌─────────────┐            ┌─────────────┐   ┌─────────────┐
               │ Customer    │            │ Inventory   │   │ Billing     │
               │ (8081)      │            │ (8082)      │   │ (8083)      │
               │ customer-svc│            │inventory-svc│   │ billing-svc │
               └─────────────┘            └─────────────┘   └─────────────┘

Notes:
- Gateway routes configured to use service discovery (Eureka) and LB URIs (e.g. `lb://CUSTOMER-SERVICE`).
- Config Server is optional; services use `spring.config.import=optional:configserver:http://localhost:9999` so they start even if the Config Server is absent.
```

Service summary table

| Component | Module | Default port | Purpose | Example route / endpoint |
|-----------|--------|--------------:|---------|--------------------------|
| Gateway | `gateway-service` | 8888 | API gateway, routes to backends | `/api/customers/**` -> `lb://CUSTOMER-SERVICE` |
| Discovery (Eureka) | `discovery-service` | 8761 | Service registry & UI | `http://localhost:8761` |
| Config Server | `config-service` | 9999 | Centralized configuration | `http://localhost:9999` |
| Customer Service | `customer-service` | 8081 | Customer CRUD (Spring Data REST) | `GET /api/customers` |
| Inventory Service | `inventory-service` | 8082 | Product/catalog endpoints | `GET /api/products` |
| Billing Service | `billing-service` | 8083 | Billing endpoints & data | `GET /api/billing` |

Components (concise)

- Gateway (`gateway-service`)
  - Purpose: API gateway / single entry point. Routes incoming HTTP requests to backend services using service discovery (Eureka) and acts as a load-balancer.
  - Default port: 8888
  - Key config: `gateway-service/src/main/resources/a.yml` (route definitions), `spring.cloud.gateway.server.webflux.discovery.locator.lower-case-service-id` enabled
  - Example routes: `/api/customers/**` -> `lb://CUSTOMER-SERVICE`, `/api/products/**` -> `lb://INVENTORY-SERVICE`
  - Actuator: `/actuator/*` available

- Discovery (`discovery-service` - Eureka server)
  - Purpose: Service registry for runtime service registration and discovery.
  - Default port: 8761
  - Key config: `eureka.client` settings in services point to `http://localhost:8761/eureka`
  - UI: Eureka dashboard at `http://localhost:8761`

- Config Server (`config-service`)
  - Purpose: Centralized externalized configuration server (Spring Cloud Config).
  - Default port: 9999
  - Default behavior in this project: configured to use a remote Git URI by default (see `config-service/src/main/resources/application.properties`).
  - Local option: can be switched to serve files from the included `config-repo/` (see Configuration section in this README).

- Customer Service (`customer-service`)
  - Purpose: Exposes customer-related CRUD and query endpoints (Spring Data REST + JPA).
  - Default port: 8081
  - API base-path: `/api` (shared property `spring.data.rest.base-path=/api`)
  - Example endpoint: `GET http://localhost:8081/api/customers` (and proxied via the gateway)
  - Notes: optional `spring.config.import` from config server

- Inventory Service (`inventory-service`)
  - Purpose: Product/catalog management endpoints.
  - Default port: 8082
  - API base-path: `/api`
  - Example endpoint: `GET http://localhost:8082/api/products` (and proxied via the gateway)
  - Notes: uses H2 in-memory DB (runtime) and can import config from the config server

- Billing Service (`billing-service`)
  - Purpose: Billing and payment-related endpoints and data.
  - Default port: 8083
  - API base-path: `/api`
  - Example: H2 JDBC example in `config-repo` uses `jdbc:h2:mem:bills-db`

Interactions & flow (short)

1. Each backend service starts and registers with Eureka (`discovery-service`) if available.
2. Services optionally import configuration from `config-service` (if the config server is running and reachable).
3. The `gateway-service` routes external client requests to backend services by looking up their location in Eureka (LB URI like `lb://SERVICE-NAME`).
4. The Eureka dashboard shows registered services and their health (Actuator endpoints provide `/actuator/health`).

Notes and troubleshooting tips (component-specific)

- If gateway routing fails, check the service names in Eureka (case sensitivity / service IDs) and the gateway route configuration in `a.yml`.
- If a service cannot find its configuration, confirm `config-service` is reachable at `http://localhost:9999` or switch the config server to use the local `config-repo`.
- Actuator endpoints are exposed (management endpoints include `*`) so health and metrics can be used to verify service health and readiness.

---

## Tech stack

- Java 17
- Spring Boot 3.5.6
- Spring Cloud 2025.0.0
- Spring Cloud Netflix Eureka (discovery)
- Spring Cloud Config (server + client)
- Spring Cloud Gateway (reactive gateway)
- Spring Data JPA, Spring Data REST, Spring HATEOAS
- H2 in-memory DB for sample data (runtime scope)
- Lombok (optional, used by some modules)
- Maven wrapper included (mvnw / mvnw.cmd)

---

## Project layout

Top-level modules (each is a Spring Boot application):

- `discovery-service` — Eureka Server (server.port=8761)
- `config-service` — Spring Cloud Config Server (server.port=9999)
- `gateway-service` — API Gateway using Spring Cloud Gateway (server.port=8888)
- `customer-service` — Customer REST service (server.port=8081)
- `inventory-service` — Inventory / Product REST service (server.port=8082)
- `billing-service` — Billing / Payment service (server.port=8083)

There is also a `config-repo/` folder containing multiple `*.properties` files intended as a config repository (local copy) and a top-level `pom.xml` for project metadata.

Key configuration files (examples):
- `config-repo/application.properties`
- `config-repo/customer-service.properties`
- `config-repo/inventory-service.properties`
- `config-repo/billing-service.properties`

Note: `config-service/src/main/resources/application.properties` is set to use a Git URL:

spring.cloud.config.server.git.uri=https://github.com/YassYassId/config-ecom-app

If you do not have that remote repo available, you can make the config server use the local `config-repo` (instructions below).

---

## Configuration details

Shared properties discovered in the repo:

- spring.data.rest.base-path=/api  (exposes Spring Data REST endpoints under `/api`)
- management.endpoints.web.exposure.include=*  (Actuator endpoints are exposed)
- eureka.client.service-url.defaultZone=http://localhost:8761/eureka (Eureka URL)
- H2 console enabled via config-repo

Config server options
- Default: `config-service` is configured to fetch from the Git URI above.
- To use the local `config-repo` folder instead, update `config-service/src/main/resources/application.properties` in one of the following ways:
  - Use the native (file) backend by adding:

    spring.cloud.config.server.native.search-locations=file:./config-repo
    spring.profiles.active=native

  - Or set the git URI to the local path (Windows example):

    spring.cloud.config.server.git.uri=file:///D:/Yassine/SDIA/S3/DistributedSystems/ecom-microservice-app/config-repo

  (Adjust the path to your local repository location.)

Service config import
Many services include the property:

spring.config.import=optional:configserver:http://localhost:9999

This means they will try to load configuration from the config server if it is reachable. If the config server is not available, the services will still start with local properties.

---

## Ports and default endpoints

- Discovery (Eureka UI): http://localhost:8761
- Config Server: http://localhost:9999
- Gateway: http://localhost:8888
- Customer Service (API): http://localhost:8081/api/... (Spring Data REST under `/api`)
- Inventory Service (API): http://localhost:8082/api/...
- Billing Service (API): http://localhost:8083/api/...

Gateway routes (configured in `gateway-service/src/main/resources/a.yml`):
- `/api/customers/**` -> `lb://CUSTOMER-SERVICE` (Customer Service - 8081)
- `/api/products/**`  -> `lb://INVENTORY-SERVICE` (Inventory Service - 8082)
- `/api/billing/**`   -> `lb://BILLING-SERVICE` (Billing Service - 8083)

Actuator endpoints are exposed (management endpoints are set to include all) — e.g., `/actuator/health` for each service.

H2 Console (enabled via config-repo): can be available at `/h2-console` depending on the running service and its configuration.

---

## How to build

Prerequisites:
- Java 17 installed and JAVA_HOME set
- Maven wrapper `mvnw.cmd` is included (Windows). You can also use a system Maven installation (`mvn`).

From the repository root (Windows cmd.exe):

- Build all modules (skip tests for faster builds):

  mvnw.cmd -T 1C clean package -DskipTests

- Or to build a single module, from root:

  mvnw.cmd -pl customer-service clean package -DskipTests

---

## How to run (recommended order)

Start services in this order (each in its own terminal) so dependencies are available:

1. discovery-service (Eureka server)
2. config-service (Config Server) — optional if using local config files
3. customer-service, inventory-service, billing-service (backend services)
4. gateway-service (API Gateway)

Notes: services have `spring.config.import=optional:configserver:http://localhost:9999`. If you don't want to run a Config Server, services will still start using their local properties.

Commands (Windows cmd.exe) — run from repository root. Each command starts that module with the Maven Spring Boot plugin.

- Run Eureka (discovery):

  mvnw.cmd -pl discovery-service spring-boot:run

- Run Config Server (if used):

  mvnw.cmd -pl config-service spring-boot:run

- Run Customer service:

  mvnw.cmd -pl customer-service spring-boot:run

- Run Inventory service:

  mvnw.cmd -pl inventory-service spring-boot:run

- Run Billing service:

  mvnw.cmd -pl billing-service spring-boot:run

- Run Gateway (last):

  mvnw.cmd -pl gateway-service spring-boot:run

Alternative: open the multi-module project in your IDE (IntelliJ IDEA / Eclipse), import as Maven project and run each `@SpringBootApplication` main class.

---

## Example requests

Assuming services are running and registered in Eureka (and gateway is up):

- List customers via gateway:

  GET http://localhost:8888/api/customers

(this forwards to the customer service registered as `CUSTOMER-SERVICE`)

- List products via gateway:

  GET http://localhost:8888/api/products

- Direct requests to services (bypass gateway):

  GET http://localhost:8081/api/customers
  GET http://localhost:8082/api/products
  GET http://localhost:8083/api/...

- Health endpoints:

  GET http://localhost:8081/actuator/health
  GET http://localhost:8888/actuator/health (gateway)

- Eureka UI:

  http://localhost:8761/  (view registered services)

---

## Troubleshooting & tips

- Config Server unreachable: services that rely on it will use local properties if `optional` is specified. If you want to ensure the service fails when config server is unavailable, remove `optional:`.
- Gateway route names: gateway uses load-balanced URIs like `lb://CUSTOMER-SERVICE`. Eureka registers services by application name (upper-case vs lower-case may matter based on configuration). If you have issues with routing, check service registration names in the Eureka UI.
- Ports conflict: change server.port in `*/src/main/resources/application.properties` for the module you want to change.
- Local config-repo usage: to make `config-service` use the local `config-repo` folder (instead of the remote Git URI), change `application.properties` in `config-service` as described in the Configuration section above.
- H2 console: the H2 console is enabled via config files; open `/h2-console` on the service port and use the JDBC URL shown in the service logs (e.g., `jdbc:h2:mem:bills-db` for billing service when started with that config).
- Lombok: some modules declare Lombok as an optional dependency. Install Lombok plugin in the IDE to avoid compilation issues in the IDE.

---

## Next steps / possible improvements

- Add Dockerfiles and a docker-compose setup to run all services locally.
- Add integration tests and a script to start all services for local testing (or use Testcontainers).
- Secure actuator endpoints and gateway routes with OAuth2 / Spring Security.
- Persist sample data to a file-backed DB for easier demo persistence.
- Add CI pipeline to build and run basic integration smoke tests.

---

## Contact / References

- Spring Boot: https://spring.io/projects/spring-boot
- Spring Cloud: https://spring.io/projects/spring-cloud
- Spring Cloud Config: https://spring.io/projects/spring-cloud-config
- Spring Cloud Gateway: https://spring.io/projects/spring-cloud-gateway
- Spring Cloud Netflix (Eureka): https://spring.io/projects/spring-cloud-netflix


---
