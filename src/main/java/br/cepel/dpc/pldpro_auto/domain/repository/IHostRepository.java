package br.cepel.dpc.pldpro_auto.domain.repository;

import br.cepel.dpc.pldpro_auto.domain.Host;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IHostRepository extends JpaRepository<Host,Long> {
    Optional<Host> findByHostName(String hostName);
}
