package it.nextsw.common.configurations;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.Projection;

/**
 *
 * @author gdm
 */
@Configuration
public class NextSdrProjectionsConfiguration {

    @Value("${nextsdr.projection.package}")
    private String projectionsPackage;

    private static final Logger log = LoggerFactory.getLogger(NextSdrProjectionsConfiguration.class);

    @Bean(name = "projectionsMap")
    public Map<String, Class> projectionsMap() throws ClassNotFoundException, IOException {
        Map<String, Class> projections = new HashMap();
        
        Set<Class<?>> projectionsSet = new Reflections(projectionsPackage).getTypesAnnotatedWith(Projection.class);

        for (Class projection: projectionsSet){
            log.info("projection trovata: " + projection.getName());
            Projection projectionAnnotation = (Projection) projection.getAnnotation(Projection.class);               
            projections.put(projectionAnnotation.name(), projection);
        }
        return projections;
    }
}
