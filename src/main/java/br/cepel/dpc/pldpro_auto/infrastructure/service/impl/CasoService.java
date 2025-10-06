package br.cepel.dpc.pldpro_auto.infrastructure.service.impl;

import br.cepel.dpc.pldpro_auto.domain.Caso;
import br.cepel.dpc.pldpro_auto.domain.repository.ICasoRepository;
import br.cepel.dpc.pldpro_auto.infrastructure.enums.StatusEnum;
import br.cepel.dpc.pldpro_auto.infrastructure.service.ICasoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CasoService implements ICasoService {

    @Autowired
    private ICasoRepository casoRepository;

    @Override
    public Caso create(String idContainer) {

        Caso caso = new Caso();
        caso.setIdContainer(idContainer);
        caso.setStatus(StatusEnum.EXECUTANDO);

        return casoRepository.save(caso);
    }
}
