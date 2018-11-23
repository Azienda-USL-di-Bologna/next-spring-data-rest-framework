package it.nextsw.common.persistence.entities.projections;

import it.nextsw.common.persistence.entities.SettoreOperativo;
import org.springframework.data.rest.core.config.Projection;

@Projection(name="agenteProjectionDefault",types={SettoreOperativo.class})
public interface SettoreOperativoProjection extends PeristentObjectProjection{

    public String getNome();

}
