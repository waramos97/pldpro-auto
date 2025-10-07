package br.cepel.dpc.pldpro_auto.dto;
import jakarta.validation.constraints.*;


public record AccessHostRequest(
        @NotBlank String hostName,
        @NotBlank String command
) {}
