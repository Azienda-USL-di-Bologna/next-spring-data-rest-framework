package it.nextsw.common.persistence.entities.projections;

import org.springframework.data.rest.core.config.Projection;

@Projection(name="persistentObjectProjectionDefault",types={PeristentObjectProjection.class})
public interface PeristentObjectProjection {

    public Long getId();
}
