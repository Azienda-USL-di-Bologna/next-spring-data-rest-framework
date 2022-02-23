package it.nextsw.common.persistence.repository;

import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.persistence.entities.Agente;
import it.nextsw.common.persistence.entities.QAgente;
import it.nextsw.common.persistence.entities.projections.AgenteProjection;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Service;


@NextSdrRepository(repositoryPath =  "sdr/agente", defaultProjection = AgenteProjection.class)
@RepositoryRestResource(exported = false, collectionResourceRel = "agentes")
public interface AgenteRestRepository extends NextSdrQueryDslRepository<Agente, Long, QAgente>, JpaRepository<Agente, Long> {


    public Agente findAgenteByNome(String nome);
}
