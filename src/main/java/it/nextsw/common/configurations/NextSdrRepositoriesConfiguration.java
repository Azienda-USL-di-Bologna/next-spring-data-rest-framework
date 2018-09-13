package it.nextsw.common.configurations;

import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import it.nextsw.common.utils.EntityReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author spritz
 */
@Configuration
public class NextSdrRepositoriesConfiguration {

//    @Autowired
//    @NextSdrRepository(repositoryPath = "albo")
//    private Albo1Repository alboRepository;
//
//    @Autowired
//    @NextSdrRepository(repositoryPath = "azienda")
//    private AziendaRepository aziendaRepository;
//
//    @Autowired
//    @NextSdrRepository(repositoryPath = "iter")
//    private IterRepository iterRepository;
//
//    @Autowired
//    @NextSdrRepository(repositoryPath = "periodoalbo")
//    private PeriodoAlboRepository periodoAlboRepository;
//
//    @Autowired
//    @NextSdrRepository(repositoryPath = "profilocommittente")
//    private ProfiloCommittenteRepository profiloCommittenteRepository;
//
//    @Autowired
//    @NextSdrRepository(repositoryPath = "provvedimento")
//    private ProvvedimentoRepository provvedimentoRepository;
//    @Value("${common.configuration.repository-package}")
//    private String repositoryPackage;

    private final String JAR_CLASS_PREFIX = "BOOT-INF.classes";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Map<String, NextSdrQueryDslRepository> repositoryMap;
    
    @Autowired
    private ListableBeanFactory beanFactory;

    private static final Logger log = LoggerFactory.getLogger(NextSdrRepositoriesConfiguration.class);

    @Bean(name = "customRepositoryMap")
    public Map<String, NextSdrQueryDslRepository> customRepositoryMap() throws ClassNotFoundException, IOException {
        Map<String, NextSdrQueryDslRepository> repositories = new HashMap();
        List<Object> repositoriesList = applicationContext.getBeansWithAnnotation(NextSdrRepository.class)
                .values().stream().collect(Collectors.toList());
        for (Object repository: repositoriesList){
            NextSdrRepository nextSdrRepositoryAnnotation = (NextSdrRepository) EntityReflectionUtils.getFirstAnnotationOverHierarchy(repository.getClass(), NextSdrRepository.class);
            if (!NextSdrQueryDslRepository.class.isAssignableFrom(repository.getClass())){
                throw new RuntimeException(String.format("La classe repository %s non estende l'interfaccia %s ",
                        repository.getClass().getName(), NextSdrQueryDslRepository.class.getName()));
            }
            repositories.put(nextSdrRepositoryAnnotation.repositoryPath(), (NextSdrQueryDslRepository) repository);
        }


//        if (StringUtils.hasText(repositoryPackage)) {
//            /**
//             * si cicla su tutte le classi repository; prima si cerca su tutto il
//             * classpath e poi si guarda se comprende il package repository
//             */
//            for (final ClassPath.ClassInfo info : ClassPath.from(ClassLoader.getSystemClassLoader()).getTopLevelClasses()) {
//                if (info.getName().contains(repositoryPackage + ".")) {
//                    Class<?> classz = null;
//                    if (info.getName().startsWith(repositoryPackage + ".")) {
//                        classz = info.load();
//                    } else if (info.getName().startsWith(JAR_CLASS_PREFIX + "." + repositoryPackage + ".")) {
//                        classz = Class.forName(info.getName().substring(JAR_CLASS_PREFIX.length() + 1));
//                    }
//                    // si vede se ha la notazione del repository
//                    RepositoryRestResource annotation = classz.getAnnotation(RepositoryRestResource.class);
//                    if (annotation != null) {
//                        repositories.put(annotation.path(), (NextSdrQueryDslRepository) repositoryMap.get(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, classz.getSimpleName())));
//                    }
//                }
//            }
//        }
        return repositories;
    }
}
