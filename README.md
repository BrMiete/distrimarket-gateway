# Distrimarket gateway

Routes Inventario (`/inventario/**`), Compras (`/compras/**`), and Ventas
(`/ventas/**`) through Eureka. Swagger at `/swagger-ui.html` uses the combined
contract from `distrimarket-common:1.0.1-SNAPSHOT`.

See [the shared library's stack guide](../distrimarket-common/README.md) for build,
database configuration, startup order, and the disposable integration check.

Run gateway tests with `mvn test` after installing the shared library. The routing
tests use real gateway rewriting and load balancing with an in-memory downstream;
they do not need running microservices or listening sockets.
