package it.nextsw.common.configurations.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author gdm
 */
public class ZoneDateTimeDeserializer extends StdDeserializer<ZonedDateTime> {

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
    public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String dateString = p.getText();
        ZonedDateTime dateTime = ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        return dateTime;
    }

   
}
