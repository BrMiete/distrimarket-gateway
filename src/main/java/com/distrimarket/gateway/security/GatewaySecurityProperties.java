package com.distrimarket.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Seguridad del gateway (prefijo {@code security} en application.yml): clave y emisor del JWT
 * (los mismos que usa el microservicio de seguridad), rutas públicas y roles por ruta.
 */
@ConfigurationProperties(prefix = "security")
public record GatewaySecurityProperties(Jwt jwt, List<String> publicPaths, List<UrlRule> rules) {

    public GatewaySecurityProperties {
        if (jwt == null) {
            throw new IllegalStateException("Falta la configuración security.jwt.");
        }
        publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    public record Jwt(String secret, String issuer) {
        public Jwt {
            if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("security.jwt.secret debe tener al menos 32 caracteres.");
            }
            if (issuer == null || issuer.isBlank()) {
                throw new IllegalStateException("security.jwt.issuer es obligatorio.");
            }
        }
    }

    /** Regla por ruta: si {@code methods} está vacío aplica a todos los métodos HTTP. */
    public record UrlRule(String pattern, List<String> methods, List<String> roles) {
        public UrlRule {
            methods = methods == null ? List.of() : List.copyOf(methods);
            roles = roles == null ? List.of() : List.copyOf(roles);
        }
    }
}
