package br.cepel.dpc.pldpro_auto.infrastructure.service.impl;

import br.cepel.dpc.pldpro_auto.domain.Caso;
import br.cepel.dpc.pldpro_auto.domain.repository.ICasoRepository;
import br.cepel.dpc.pldpro_auto.infrastructure.enums.StatusEnum;
import br.cepel.dpc.pldpro_auto.infrastructure.event.CasoCreatedEvent;
import br.cepel.dpc.pldpro_auto.infrastructure.service.ICasoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CasoService implements ICasoService {

    @Autowired
    private ICasoRepository casoRepository;

    @Autowired
    private ApplicationEventPublisher publisher;


    @Override
    @Transactional
    public Caso create(String idContainer) {

        Caso caso = new Caso();
        caso.setIdContainer(idContainer);
        caso.setStatus(StatusEnum.EXECUTANDO);

        Caso saved = casoRepository.save(caso);
        publisher.publishEvent(new CasoCreatedEvent(saved.getId()));

        return saved;
    }
}
