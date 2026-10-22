package com.distrimarket.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "COMPRAS_URL=http://compras.test:8083",
        "INVENTARIO_URL=http://inventario.test:8081",
        "VENTAS_URL=http://ventas.test:8082",
        "AUTH_URL=http://auth.test:8084"
})
@TestExecutionListeners(listeners = DependencyInjectionTestExecutionListener.class, inheritListeners = false)
class GatewayRoutingTest {
    @Autowired
    private ApplicationContext context;
    private WebTestClient client;

    @BeforeEach
    void client() {
        client = WebTestClient.bindToApplicationContext(context).build();
    }

    @Test
    void forwardsEachServicePrefixAndQuery() {
        for (String[] route : new String[][] {
                {"inventario", "productos", "inventario.test:8081"},
                {"compras", "facturas", "compras.test:8083"},
                {"ventas", "facturas-venta", "ventas.test:8082"},
                {"ventas", "facturas", "ventas.test:8082"}
        }) {
            String path = "/" + route[0] + "/" + route[1] + "?page=1&size=2&sort=id,desc";
            client.get().uri(path).exchange().expectStatus().isOk()
                    .expectBody(String.class).isEqualTo("GET http://" + route[2] + path + " ");
        }
    }

    @Test
    void forwardsAuthPaths() {
        client.get().uri("/auth/usuarios?page=0&size=2")
                .exchange().expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("GET http://auth.test:8084/auth/usuarios?page=0&size=2 ");
    }

    @Test
    void forwardsRequestBodiesAndCancellationMethods() {
        client.post().uri("/compras/facturas").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"numeroFactura\":\"001-001-0000001\"}")
                .exchange().expectStatus().isOk().expectBody(String.class)
                .isEqualTo("POST http://compras.test:8083/compras/facturas {\"numeroFactura\":\"001-001-0000001\"}");
        client.patch().uri("/ventas/facturas/12").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"estado\":\"ANULADA\"}")
                .exchange().expectStatus().isOk().expectBody(String.class)
                .isEqualTo("PATCH http://ventas.test:8082/ventas/facturas/12 {\"estado\":\"ANULADA\"}");
    }

    @Test
    void rewritesOnlyComprasDocumentationAndLegacyPaymentPaths() {
        client.get().uri("/compras/v3/api-docs").exchange().expectStatus().isOk()
                .expectBody(String.class).isEqualTo("GET http://compras.test:8083/v3/api-docs ");
        client.get().uri("/compras/v3/api-docs.yaml").exchange().expectStatus().isOk()
                .expectBody(String.class).isEqualTo("GET http://compras.test:8083/v3/api-docs.yaml ");
        client.get().uri("/ventas/v3/api-docs").exchange().expectStatus().isOk()
                .expectBody(String.class).isEqualTo("GET http://ventas.test:8082/ventas/v3/api-docs ");
        client.get().uri("/inventario/v3/api-docs").exchange().expectStatus().isOk()
                .expectBody(String.class).isEqualTo("GET http://inventario.test:8081/inventario/v3/api-docs ");
        client.get().uri("/medios-pago/4?size=2").exchange().expectStatus().isOk()
                .expectBody(String.class).isEqualTo("GET http://ventas.test:8082/ventas/medios-pago/4?size=2 ");
    }

    @Test
    void servesTheCombinedSwaggerContract() {
        client.get().uri("/openapi.yaml").exchange().expectStatus().isOk()
                .expectBody(String.class).value(contract -> {
                    org.assertj.core.api.Assertions.assertThat(contract)
                            .contains("url: http://localhost:8080", "/compras/facturas:",
                                    "/inventario/productos:", "/ventas/facturas-venta:",
                                    "/ventas/facturas:", "/auth/login:");
                });
        client.get().uri("/v3/api-docs/swagger-config").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.url").isEqualTo("/openapi.yaml");
        URI swagger = client.get().uri("/swagger-ui.html").exchange().expectStatus().is3xxRedirection()
                .returnResult(Void.class).getResponseHeaders().getLocation();
        client.get().uri(swagger).exchange().expectStatus().isOk();
    }

    @TestConfiguration
    static class DownstreamConfiguration {
        @Bean
        GlobalFilter downstream() {
            return new InMemoryDownstream();
        }
    }

    // Runs after real route rewriting and load balancing, before the network filter.
    static class InMemoryDownstream implements GlobalFilter, Ordered {
        @Override
        public int getOrder() {
            return 11000;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
            URI uri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
            return DataBufferUtils.join(exchange.getRequest().getBody())
                    .map(buffer -> {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        DataBufferUtils.release(buffer);
                        return new String(bytes, StandardCharsets.UTF_8);
                    })
                    .defaultIfEmpty("")
                    .flatMap(body -> {
                        String text = exchange.getRequest().getMethod() + " " + uri + " " + body;
                        exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
                        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory()
                                .wrap(text.getBytes(StandardCharsets.UTF_8))));
                    });
        }
    }
}
