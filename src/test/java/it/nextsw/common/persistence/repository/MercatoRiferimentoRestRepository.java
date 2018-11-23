package it.nextsw.common.persistence.repository;

import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.persistence.entities.MercatoRiferimento;
import it.nextsw.common.persistence.entities.QMercatoRiferimento;
import it.nextsw.common.persistence.entities.projections.MercatoRiferimentoProjection;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


@NextSdrRepository(repositoryPath =  "sdr/mercato-riferimento", defaultProjection = MercatoRiferimentoProjection.class)
@RepositoryRestResource(exported = false, collectionResourceRel = "settoriOperativi")
public interface MercatoRiferimentoRestRepository extends NextSdrQueryDslRepository<MercatoRiferimento, Long, QMercatoRiferimento>, JpaRepository<MercatoRiferimento, Long> {

}
