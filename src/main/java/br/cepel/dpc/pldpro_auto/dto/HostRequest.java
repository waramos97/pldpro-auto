package br.cepel.dpc.pldpro_auto.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record HostRequest(
        @NotBlank String host,
        @Min(1) @Max(65535) Integer port,
        @NotBlank String user,
        Boolean bootstrap,
        @NotBlank String password
) {}
