package br.cepel.dpc.pldpro_auto.domain.repository;

import br.cepel.dpc.pldpro_auto.domain.Caso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ICasoRepository extends JpaRepository<Caso,Long> {
}
