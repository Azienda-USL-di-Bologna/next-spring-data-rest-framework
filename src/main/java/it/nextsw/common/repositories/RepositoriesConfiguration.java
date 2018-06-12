package it.nextsw.common.repositories;

import com.google.common.base.CaseFormat;
import com.google.common.reflect.ClassPath;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 *
 * @author Utente
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

    @Autowired
    private Map<String, CustomQueryDslRepository> repositoryMap;

    @Bean(name = "customRepositoryMap")
    public Map<String, CustomQueryDslRepository> customRepositoryMap() throws ClassNotFoundException, IOException {
        Map<String, CustomQueryDslRepository> repositories = new HashMap();
        for (final ClassPath.ClassInfo info : ClassPath.from(ClassLoader.getSystemClassLoader()).getTopLevelClasses()) {
            if (info.getName().startsWith(repositoryPackage + ".")) {
                Class<?> classz = info.load();
                RepositoryRestResource annotation = classz.getAnnotation(RepositoryRestResource.class);
                if (annotation != null) {
                    repositories.put(annotation.path(), (CustomQueryDslRepository) repositoryMap.get(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, classz.getSimpleName())));
                }
            }
        }
        return repositories;
    }
}
