package it.nextsw.common.configurations.jackson;

import java.io.IOException;
import java.time.ZonedDateTime;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.SerializationContext;

import org.hibernate.type.descriptor.DateTimeUtils;

/**
 *
 * @author gdm
 */
public class ZoneDateTimeSerializer extends StdSerializer<ZonedDateTime> {
    
    public ZoneDateTimeSerializer(Class<ZonedDateTime> vc) {
        super(vc);
    }

    public ZoneDateTimeSerializer(JavaType valueType) {
        super(valueType);
    }

    public ZoneDateTimeSerializer(StdSerializer src) {
        super(src);
    }

    @Override
    public void serialize(ZonedDateTime value, JsonGenerator generator, SerializationContext provider) {
        if (value != null) {
            generator.writeString(DateTimeUtils.DATE_TIME_FORMATTER_TIMESTAMP_WITH_MILLIS_AND_OFFSET.format(value));
        } else {
            generator.writeNull();
        }
    }

   
}
