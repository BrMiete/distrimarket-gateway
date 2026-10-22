package com.distrimarket.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * Agrega al pedido que se reenvía quién es el usuario ya validado (solo informativo, para logs).
 * Los encabezados que mande el cliente se descartan; el token original sigue viajando y cada
 * microservicio lo vuelve a validar para autorizar.
 */
@Component
public class UsuarioAutenticadoHeadersFilter implements GlobalFilter, Ordered {

    public static final String HEADER_USUARIO = "X-Auth-Usuario";
    public static final String HEADER_ROLES = "X-Auth-Roles";

    private static final Logger log = LoggerFactory.getLogger(UsuarioAutenticadoHeadersFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest limpio = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HEADER_USUARIO);
                    headers.remove(HEADER_ROLES);
                })
                .build();
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(auth -> {
                    String roles = auth.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(","));
                    log.info("Gateway: {} {} usuario={} roles=[{}]",
                            limpio.getMethod(), limpio.getPath(), auth.getName(), roles);
                    return limpio.mutate().header(HEADER_USUARIO, auth.getName()).header(HEADER_ROLES, roles).build();
                })
                .defaultIfEmpty(limpio)
                .flatMap(request -> chain.filter(exchange.mutate().request(request).build()));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
