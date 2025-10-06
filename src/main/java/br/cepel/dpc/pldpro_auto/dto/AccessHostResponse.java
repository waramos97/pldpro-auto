package br.cepel.dpc.pldpro_auto.dto;

public record AccessHostResponse(
        int exitCode,
        String stdout,
        String stderr
) {}