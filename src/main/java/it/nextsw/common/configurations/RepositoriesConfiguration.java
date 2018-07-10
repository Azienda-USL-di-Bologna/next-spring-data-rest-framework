package it.nextsw.common.configurations;

import com.google.common.base.CaseFormat;
import com.google.common.reflect.ClassPath;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author spritz
 */
@Configuration
public class RepositoriesConfiguration {

//    @Autowired
//    @RepositoryDescriptor(repositoryPath = "albo")
//    private Albo1Repository alboRepository;
//
//    @Autowired
//    @RepositoryDescriptor(repositoryPath = "azienda")
//    private AziendaRepository aziendaRepository;
//
//    @Autowired
//    @RepositoryDescriptor(repositoryPath = "iter")
//    private IterRepository iterRepository;
//
//    @Autowired
//    @RepositoryDescriptor(repositoryPath = "periodoalbo")
//    private PeriodoAlboRepository periodoAlboRepository;
//
//    @Autowired
//    @RepositoryDescriptor(repositoryPath = "profilocommittente")
//    private ProfiloCommittenteRepository profiloCommittenteRepository;
//
//    @Autowired
//    @RepositoryDescriptor(repositoryPath = "provvedimento")
//    private ProvvedimentoRepository provvedimentoRepository;
    @Value("${common.configuration.repository-package}")
    private String repositoryPackage;

    private final String JAR_CLASS_PREFIX = "BOOT-INF.classes";

    @Autowired
    private Map<String, CustomQueryDslRepository> repositoryMap;
    
     @Autowired
    private ListableBeanFactory beanFactory;

    private static final Logger log = LoggerFactory.getLogger(RepositoriesConfiguration.class);

    @Bean(name = "customRepositoryMap")
    public Map<String, CustomQueryDslRepository> customRepositoryMap() throws ClassNotFoundException, IOException {
        Map<String, CustomQueryDslRepository> repositories = new HashMap();

        /**
         * si cicla su tutte le classi repository; prima si cerca su tutto il
         * classpath e poi si guarda se comprende il package repository
         */
        for (final ClassPath.ClassInfo info : ClassPath.from(ClassLoader.getSystemClassLoader()).getTopLevelClasses()) {
            if (info.getName().contains(repositoryPackage + ".")) {
                Class<?> classz = null;
                if (info.getName().startsWith(repositoryPackage + ".")) {
//                    log.info("trovato repository: " + info.getName());
                    classz = info.load();
//                    log.info("loading class: " + info.getName());
                } else if (info.getName().startsWith(JAR_CLASS_PREFIX + "." + repositoryPackage + ".")) {
//                    log.info("loading class: " + info.getName().substring(JAR_CLASS_PREFIX.length() + 1));
                    classz = Class.forName(info.getName().substring(JAR_CLASS_PREFIX.length() + 1));
                }
                // si vede se ha la notazione del repository
                RepositoryRestResource annotation = classz.getAnnotation(RepositoryRestResource.class);
                if (annotation != null) {
                    repositories.put(annotation.path(), (CustomQueryDslRepository) repositoryMap.get(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, classz.getSimpleName())));
                }
            }
        }
        return repositories;
    }
}
