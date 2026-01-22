package it.nextsw.common.configurations.jackson;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    
    @Bean
    public JsonMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();

            module.addDeserializer(ZonedDateTime.class, new ZoneDateTimeDeserializer(ZonedDateTime.class));
            module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(LocalDateTime.class));
            module.addDeserializer(LocalDate.class, new LocalDateDeserializer(LocalDate.class));

            builder.addModule(module);
        };
    }
}
