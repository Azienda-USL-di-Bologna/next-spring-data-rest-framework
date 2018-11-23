package it.nextsw.common.persistence.entities.projections;

import it.nextsw.common.persistence.entities.Agente;
import it.nextsw.common.persistence.entities.AreaOperativa;
import org.springframework.data.rest.core.config.Projection;

@Projection(name="agenteProjectionDefault",types={AreaOperativa.class})
public interface AreaOperativaProjection extends PeristentObjectProjection{

    public String getAreaNielsen();

    public String getRegione();

    public String getProvincia();

    public Agente getAgente();



}
