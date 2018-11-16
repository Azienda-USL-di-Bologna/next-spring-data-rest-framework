package it.nextsw.common.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.nextsw.common.configs.spring.SpringConfig;
import it.nextsw.common.persistence.entities.Agente;
import it.nextsw.common.persistence.entities.QAgente;
import it.nextsw.common.rest.CrudController;
import it.nextsw.common.rest.QueringRepositorySpringController;
import it.nextsw.common.test.CreateEntityService;
import it.nextsw.common.test.TestConstants;
import it.nextsw.common.test.TestUtils;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootApplication(scanBasePackages = {"it.nextsw"})
@EntityScan("it.nextsw.common.persistence.entities")
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties")
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringConfig.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AgenteTest {


    @Autowired
    private CreateEntityService createEntityService;
    @Autowired
    private CrudController crudController;
    @Autowired
    private QueringRepositorySpringController queringRepositorySpringController;
    @Autowired
    private TestUtils testUtils;
    @Autowired
    protected ObjectMapper objectMapper;


    @Before
    public void setup () {
        createEntityService.createDBEnviromentIfNotExist();
//        DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(applicationContext.);
//        this.mockMvc = builder.build();
    }

    @Test
    public void test1createEnviroment() throws Exception {

    }

    @Test
    public void test2AgenteGet() throws Exception {
        createEntityService.createDBEnviromentIfNotExist();
        ResponseEntity responseEntity = queringRepositorySpringController.agenteQuering(
                QAgente.agente.nome.eq(TestConstants.NOME_AGENTE_DEFAULT),
                PageRequest.of(0, 20), null, null,
                testUtils.createHttpServletRequest(RequestMethod.PATCH, TestConstants.AGENTE_PATH_DEFAULT), null);
        Object body = responseEntity.getBody();
    }



    @Test
    public void test3UpdateAgenteUpdate() throws Exception {
        createEntityService.createDBEnviromentIfNotExist();

        Agente agente = createEntityService.getAgenteDefaultfromDB();
        Agente agenteTest = new Agente();

        //creo un agente con solo un settore operativo
        agenteTest.setSettori(agente.getSettori().stream()
                .filter(settoreOperativo -> settoreOperativo.getNome().equals(TestConstants.NOME_SETTORE_OPERATIVO_DEFAULT))
                .collect(Collectors.toList()));


        ResponseEntity responseEntity = crudController.updateResource(agente.getId(), testUtils.serializeInMap(agenteTest),
                testUtils.createHttpServletRequest(RequestMethod.PATCH, TestConstants.AGENTE_PATH_DEFAULT), null);
        agente = createEntityService.getAgenteDefaultfromDB();


        int i =0;
    }

}

