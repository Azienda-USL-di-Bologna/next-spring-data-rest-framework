package it.nextsw.common.configurations;

import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import it.nextsw.common.utils.CommonUtils;
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

    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private CommonUtils commonUtils;

    private static final Logger log = LoggerFactory.getLogger(NextSdrRepositoriesConfiguration.class);

    @Bean(name = "customRepositoryPathMap")
    public Map<String, NextSdrQueryDslRepository> customRepositoryPathMap() throws ClassNotFoundException, IOException {
        Map<String, NextSdrQueryDslRepository> repositories = new HashMap();
        List<Object> repositoriesList = applicationContext.getBeansWithAnnotation(NextSdrRepository.class)
                .values().stream().collect(Collectors.toList());
        for (Object repository : repositoriesList) {
            NextSdrRepository nextSdrRepositoryAnnotation = (NextSdrRepository) EntityReflectionUtils.getFirstAnnotationOverHierarchy(repository.getClass(), NextSdrRepository.class);
            if (!NextSdrQueryDslRepository.class.isAssignableFrom(repository.getClass())) {
                throw new RuntimeException(String.format("La classe repository %s non estende l'interfaccia %s ",
                        repository.getClass().getName(), NextSdrQueryDslRepository.class.getName()));
            }
//            repositories.put(nextSdrRepositoryAnnotation.repositoryPath(), (NextSdrQueryDslRepository) repository);
            repositories.put(commonUtils.resolvePlaceHolder(nextSdrRepositoryAnnotation.repositoryPath()), (NextSdrQueryDslRepository) repository);
        }
        return repositories;
    }
    
    @Bean(name = "customRepositoryEntityMap")
    public Map<String, NextSdrQueryDslRepository> customRepositoryEntityMap() throws ClassNotFoundException, IOException {
        Map<String, NextSdrQueryDslRepository> repositories = new HashMap();
        List<Object> repositoriesList = applicationContext.getBeansWithAnnotation(NextSdrRepository.class)
                .values().stream().collect(Collectors.toList());
        for (Object repository: repositoriesList){
            if (!NextSdrQueryDslRepository.class.isAssignableFrom(repository.getClass())){
                throw new RuntimeException(String.format("La classe repository %s non estende l'interfaccia %s ",
                        repository.getClass().getName(), NextSdrQueryDslRepository.class.getName()));
            }
            Class entityClass = EntityReflectionUtils.getEntityClassFromRepository(repository);
            repositories.put(entityClass.getCanonicalName(), (NextSdrQueryDslRepository) repository);
        }
        return repositories;
    }
}
