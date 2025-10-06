package br.cepel.dpc.pldpro_auto.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record MultiAccessHostRequest(
        @NotBlank String host,
        @NotNull @Min(1) @Max(65535) Integer port,
        @NotBlank String user,
        String commandPrefix,                // opcional: se quiser um prefixo comum
        Boolean bootstrap,
        String password,
        @NotEmpty List<@NotBlank String> paths   // lista de caminhos remotos
) {}