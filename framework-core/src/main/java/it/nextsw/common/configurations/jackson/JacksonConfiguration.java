package it.nextsw.common.configurations.jackson;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author gdm
 */
@Configuration
public class JacksonConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
//        SimpleModule s = new SimpleModule();
//        s.addDeserializer(ZonedDateTime.class, new ZoneDateTimeDeserializer(ZonedDateTime.class));
        return builder -> builder
//                .serializerByType(ZonedDateTime.class, new ZoneDateTimeSerializer(ZonedDateTime.class))
                .deserializerByType(ZonedDateTime.class, new ZoneDateTimeDeserializer(ZonedDateTime.class))
                .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(LocalDateTime.class))
                .deserializerByType(LocalDate.class, new LocalDateDeserializer(LocalDate.class));
    }
}
