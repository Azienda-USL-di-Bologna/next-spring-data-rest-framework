package it.nextsw.common.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.nextsw.common.configs.spring.SpringConfig;
import it.nextsw.common.persistence.entities.Agente;
import it.nextsw.common.persistence.entities.QAgente;
import it.nextsw.common.persistence.repository.AgenteRestRepository;
import it.nextsw.common.rest.CrudController;
import it.nextsw.common.rest.QueringRepositorySpringController;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.web.config.QuerydslWebConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;


import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootApplication(scanBasePackages = {"it.nextsw"})
@EntityScan("it.nextsw.common.persistence.entities")
@EnableJpaRepositories(basePackages = {"it.nextsw.common.persistence.repository"})
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties")
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringConfig.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AgenteTest {


    @Autowired
    private CreateEntityService createEntityService;
    @Autowired
    private AgenteRestRepository agenteRestRepository;
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
    public void test3AgenteGet() throws Exception {
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        Object resp = restTemplate.getForEntity("http://localhost:8080"+TestConstants.AGENTE_PATH_DEFAULT, Agente.class);
        int i =0;
    }



//    @Test
//    public void test3UpdateAgenteUpdate() throws Exception {
//        createEntityService.createDBEnviromentIfNotExist();
//
//        Agente agente = createEntityService.getAgenteDefaultfromDB();
//        Agente agenteTest = new Agente();
//
//        //creo un agente con solo un settore operativo
//        agenteTest.setSettori(agente.getSettori().stream()
//                .filter(settoreOperativo -> settoreOperativo.getNome().equals(TestConstants.NOME_SETTORE_OPERATIVO_DEFAULT))
//                .collect(Collectors.toList()));
//
//
//        ResponseEntity responseEntity = crudController.updateResource(agente.getId(), testUtils.serializeInMap(agenteTest),
//                testUtils.createHttpServletRequest(RequestMethod.PATCH, TestConstants.AGENTE_PATH_DEFAULT), null);
//        agente = createEntityService.getAgenteDefaultfromDB();
//
//
//        int i =0;
//    }

    @Test
    public void test4UpdateAgenteUpdate() throws Exception {
        createEntityService.createDBEnviromentIfNotExist();

        Agente agente = createEntityService.getAgenteDefaultfromDB();
        Agente agenteTest = new Agente();

        //creo un agente con solo un settore operativo
        agenteTest.setSettori(agente.getSettori().stream()
                .filter(settoreOperativo -> settoreOperativo.getNome().equals(TestConstants.NOME_SETTORE_OPERATIVO_DEFAULT))
                .collect(Collectors.toList()));

        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        Object resp = restTemplate.patchForObject("http://localhost:8080"+TestConstants.AGENTE_PATH_DEFAULT+"/"+agente.getId(),
                agenteTest, Object.class);
        agente = createEntityService.getAgenteDefaultfromDB();


    }
}

