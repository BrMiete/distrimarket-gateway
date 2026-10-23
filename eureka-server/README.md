# distrimarket-eureka

Servidor Eureka de Distrimarket, dentro del repositorio del gateway.
Escucha en `http://localhost:8761` y no necesita base de datos.

```sh
mvn spring-boot:run
```

En el stack completo lo arranca `distrimarket-common/scripts/run_stack.py`
antes que los microservicios.
