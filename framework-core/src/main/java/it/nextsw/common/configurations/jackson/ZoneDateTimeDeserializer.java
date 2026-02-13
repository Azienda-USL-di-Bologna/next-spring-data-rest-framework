package it.nextsw.common.configurations.jackson;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.core.JacksonException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author gdm
 */
public class ZoneDateTimeDeserializer extends StdDeserializer<ZonedDateTime> {

    public ZoneDateTimeDeserializer() {
        super(ZonedDateTime.class);
    }
    
    public ZoneDateTimeDeserializer(Class<ZonedDateTime> vc) {
        super(vc);
    }

    public ZoneDateTimeDeserializer(JavaType valueType) {
        super(valueType);
    }

    public ZoneDateTimeDeserializer(StdDeserializer src) {
        super(src);
    }

    @Override
    public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String dateString = p.getString();
        ZonedDateTime dateTime = ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        return dateTime;
    }

   
}
