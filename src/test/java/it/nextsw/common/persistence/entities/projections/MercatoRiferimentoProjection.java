package it.nextsw.common.persistence.entities.projections;

import it.nextsw.common.persistence.entities.MercatoRiferimento;
import it.nextsw.common.persistence.entities.SettoreOperativo;
import org.springframework.data.rest.core.config.Projection;

@Projection(name="agenteProjectionDefault",types={MercatoRiferimento.class})
public interface MercatoRiferimentoProjection extends PeristentObjectProjection{

    public String getNome();

}
