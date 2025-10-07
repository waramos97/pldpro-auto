package br.cepel.dpc.pldpro_auto.domain.repository;

import br.cepel.dpc.pldpro_auto.domain.Caso;
import br.cepel.dpc.pldpro_auto.infrastructure.enums.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ICasoRepository extends JpaRepository<Caso,Long> {
    List<Caso> findByStatusIn(List<StatusEnum> allStatus);
}
