package com.distrimarket.gateway.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class OpenApiController {

    private final Resource openApiYaml = new ClassPathResource("static/openapi.yaml");

    @GetMapping(value = "/openapi.yaml", produces = "application/yaml;charset=UTF-8")
    public ResponseEntity<Resource> getOpenApiSpec() {
        if (!openApiYaml.exists()) {
            // Si no está en static/, busca directo en la raíz del classpath
            Resource fallback = new ClassPathResource("openapi.yaml");
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"openapi.yaml\"")
                    .body(fallback);
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"openapi.yaml\"")
                .body(openApiYaml);
    }
}