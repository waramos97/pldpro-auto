package br.cepel.dpc.pldpro_auto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public io.swagger.v3.oas.models.OpenAPI apiInfo() {
        return new io.swagger.v3.oas.models.OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("PLDPro Auto")
                        .version("v1")
                        .description("Endpoints para a automação do PLDPro")
                        .contact(new io.swagger.v3.oas.models.info.Contact()
                                .name("Departamento DPC")));
    }
}

