package br.cepel.dpc.pldpro_auto.dto;
import jakarta.validation.constraints.*;


public record AccessHostRequest(
        @NotBlank String host,
        @Min(1) @Max(65535) Integer port,
        @NotBlank String user,
        String command,      // opcional
        Boolean bootstrap,   // true = 1º acesso: instalar chave
        String password      // obrigatório se bootstrap=true
) {}
