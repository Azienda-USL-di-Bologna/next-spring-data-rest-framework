package it.nextsw.common.test;


import com.fasterxml.jackson.core.JsonProcessingException;
import it.nextsw.common.persistence.entities.*;
import it.nextsw.common.persistence.repository.AgenteRestRepository;
import it.nextsw.common.persistence.repository.MercatoRiferimentoRestRepository;
import it.nextsw.common.persistence.repository.SettoreOperativoRestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class CreateEntityService {

    @Autowired
    private AgenteRestRepository agenteRestRepository;
    @Autowired
    private SettoreOperativoRestRepository settoreOperativoRestRepository;
    @Autowired
    private MercatoRiferimentoRestRepository mercatoRiferimentoRestRepository;
    @Autowired
    private TestUtils testUtils;

    protected boolean enviromentCreated = false;

    /**
     * crea l'ambiente solo la prima volta che viene chiamato questo emtodo
     */
    @Transactional
    public void createDBEnviromentIfNotExist() {
        if (!enviromentCreated)
            createDBEnviroment();
    }

    /**
     * Crea un minimo di ambiente con entità di test
     */
    @Transactional
    public void createDBEnviroment() {
        List<SettoreOperativo> settoriOperativi = createSettoriOperativi(Arrays.asList(
                TestConstants.NOME_SETTORE_OPERATIVO_DEFAULT, TestConstants.NOME_SETTORE_OPERATIVO2_DEFAULT));
        MercatoRiferimento mercatoRiferimento = createMercatoRiferimento();
        Agente agente = createAgente(settoriOperativi, mercatoRiferimento);

        enviromentCreated = true;
    }

    @Transactional
    public List<SettoreOperativo> createSettoriOperativi(List<String> names){
        List<SettoreOperativo> result = new ArrayList<>();
        names.stream().forEach(nome -> {
            SettoreOperativo settoreOperativo = new SettoreOperativo();
            settoreOperativo.setNome(nome);
            settoreOperativo = settoreOperativoRestRepository.save(settoreOperativo);
            result.add(settoreOperativo);
        });
        return result;
    }

    @Transactional
    public MercatoRiferimento createMercatoRiferimento(){
        MercatoRiferimento mercatoRiferimento = new MercatoRiferimento();
        mercatoRiferimento.setNome("MercatoRiferimento1");
        mercatoRiferimento = mercatoRiferimentoRestRepository.save(mercatoRiferimento);
        return mercatoRiferimento;
    }

    @Transactional
    public AreaOperativa createAreaOperativa() {
        AreaOperativa areaOperativa = new AreaOperativa();
        areaOperativa.setAreaNielsen("Area 1");
        areaOperativa.setProvincia("BO");
        areaOperativa.setRegione("Emilia");
       // areaOperativa.setAgente();

        return areaOperativa;
    }

    public Agente createAgente(List<SettoreOperativo> settoriOperativi, MercatoRiferimento mercatoRiferimento) {
        Agente agente = new Agente();
        agente.setNome(TestConstants.NOME_AGENTE_DEFAULT);
        agente.setCognome("Bellissimo");
        agente.setLivelloPrivacyAgente(LivelloPrivacyAgente.ALTA);
        agente.setSettori(settoriOperativi != null ? settoriOperativi : new ArrayList<>());
        agente.setMercatoRiferimento(mercatoRiferimento!=null ? mercatoRiferimento : null);
        agente = agenteRestRepository.save(agente);
        return agente;
    }

    @Transactional
    public Agente getAgenteDefaultfromDB() throws JsonProcessingException {
        Agente agente = agenteRestRepository.findAgenteByNome(TestConstants.NOME_AGENTE_DEFAULT);
        testUtils.serializeInMap(agente);
        return agente;
    }


}
