# Distrimarket gateway

Routes Inventario (`/inventario/**`), Compras (`/compras/**`), Ventas
(`/ventas/**`), and Auth (`/auth/**`) through Eureka with `lb://` URIs.
Swagger at `/swagger-ui.html` uses the combined contract from
`distrimarket-common:1.0.1-SNAPSHOT`.

This repository also contains the Eureka server as a standalone application in
`eureka-server/` (port 8761). `build_stack.py` builds both applications.

See [the shared library's stack guide](../distrimarket-common/README.md) for build,
database configuration, startup order, and the disposable integration check.

Run gateway tests with `mvn test` after installing the shared library. The routing
tests use `eureka.client.enabled=false` and an in-memory discovery client; they do
not need running microservices or listening sockets.
