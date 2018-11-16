package it.nextsw.common.persistence.entities.projections;

import it.nextsw.common.persistence.entities.Agente;
import org.springframework.data.rest.core.config.Projection;

@Projection(name="agenteProjectionDefault",types={Agente.class})
public interface AgenteProjection extends PeristentObjectProjection{

    public String getNome();


}
