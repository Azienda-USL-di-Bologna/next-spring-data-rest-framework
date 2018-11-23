package it.nextsw.common.persistence.repository;

import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.persistence.entities.QSettoreOperativo;
import it.nextsw.common.persistence.entities.SettoreOperativo;
import it.nextsw.common.persistence.entities.projections.SettoreOperativoProjection;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


@NextSdrRepository(repositoryPath =  "sdr/settore-operativo", defaultProjection = SettoreOperativoProjection.class)
@RepositoryRestResource(exported = false, collectionResourceRel = "settoriOperativi")
public interface SettoreOperativoRestRepository extends NextSdrQueryDslRepository<SettoreOperativo, Long, QSettoreOperativo>, JpaRepository<SettoreOperativo, Long> {

}
