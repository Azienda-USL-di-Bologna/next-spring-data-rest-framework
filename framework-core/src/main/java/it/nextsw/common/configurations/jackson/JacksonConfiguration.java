package it.nextsw.common.configurations.jackson;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.jackson2.autoconfigure.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.ConstructorDetector;
import tools.jackson.databind.module.SimpleModule;

/**
 *
 * @author gdm
 */
@Configuration
public class JacksonConfiguration {

//    @Bean
//    public Module dateTimeModule() {
//        SimpleModule module = new SimpleModule();
//        module.addDeserializer(ZonedDateTime.class, new ZoneDateTimeDeserializer());
//        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
//        module.addDeserializer(LocalDate.class, new LocalDateDeserializer());
//        return module;
//    }
    
    /**
     * temporaneo, fino a che anche hypersistence-utils-hibernate-71 non passa a jackson3
     * @return 
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer json2Customizer() {
        return builder -> builder
            .modules(new JavaTimeModule());
//            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    @Bean
    public JsonMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();

//            module.addDeserializer(ZonedDateTime.class, new ZoneDateTimeDeserializer(ZonedDateTime.class));
//            module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(LocalDateTime.class));
//            module.addDeserializer(LocalDate.class, new LocalDateDeserializer(LocalDate.class));
//
//            builder.addModule(module);
//            JavaTimeModule javaTimeModule = new JavaTimeModule();
//            builder.addModule(javaTimeModule);
            //builder.enable(MapperFeature.)
            builder.enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION);
            builder.constructorDetector(ConstructorDetector.DEFAULT.withAllowImplicitWithDefaultConstructor(false));
        };
    }
}
