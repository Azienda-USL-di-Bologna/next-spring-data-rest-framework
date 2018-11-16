package it.nextsw.common.persistence.repository;

import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.persistence.entities.AreaOperativa;
import it.nextsw.common.persistence.entities.QAreaOperativa;
import it.nextsw.common.persistence.entities.projections.AreaOperativaProjection;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


@NextSdrRepository(repositoryPath =  "sdr/area-operativa", defaultProjection = AreaOperativaProjection.class)
@RepositoryRestResource(exported = false, collectionResourceRel = "areeOperative")
public interface AreaOperativaRestRepository extends NextSdrQueryDslRepository<AreaOperativa, Long, QAreaOperativa>, JpaRepository<AreaOperativa, Long> {

}
