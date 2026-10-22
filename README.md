# Distrimarket gateway

Routes Inventario (`/inventario/**`), Compras (`/compras/**`), Ventas
(`/ventas/**`), and Auth (`/auth/**`) to static URLs. There is no discovery
server. Swagger at `/swagger-ui.html` uses the combined contract from
`distrimarket-common:1.0.1-SNAPSHOT`.

Route targets default to `localhost` ports and can be overridden with
`INVENTARIO_URL`, `COMPRAS_URL`, `VENTAS_URL`, and `AUTH_URL`.

See [the shared library's stack guide](../distrimarket-common/README.md) for build,
database configuration, startup order, and the disposable integration check.

Run gateway tests with `mvn test` after installing the shared library. The routing
tests use real gateway rewriting with an in-memory downstream; they do not need
running microservices or listening sockets.
