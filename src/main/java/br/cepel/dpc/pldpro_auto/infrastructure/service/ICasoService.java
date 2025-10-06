package br.cepel.dpc.pldpro_auto.infrastructure.service;

import br.cepel.dpc.pldpro_auto.domain.Caso;

public interface ICasoService {
    Caso create(String idContainer);
}
