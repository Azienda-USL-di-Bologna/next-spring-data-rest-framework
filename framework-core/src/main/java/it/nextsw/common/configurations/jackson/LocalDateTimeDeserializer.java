package it.nextsw.common.configurations.jackson;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.core.JacksonException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author gdm
 */
public class LocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

    public LocalDateTimeDeserializer() {
        super(LocalDateTime.class);
    }
    
    public LocalDateTimeDeserializer(Class<LocalDateTime> vc) {
        super(vc);
    }

    public LocalDateTimeDeserializer(JavaType valueType) {
        super(valueType);
    }

    public LocalDateTimeDeserializer(StdDeserializer src) {
        super(src);
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String dateString = p.getText();
        LocalDateTime dateTime = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return dateTime;
    }

}
