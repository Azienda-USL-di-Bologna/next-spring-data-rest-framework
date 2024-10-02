package it.nextsw.common.configurations.jackson;

import java.io.IOException;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
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
    public void serialize(ZonedDateTime value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        if (value != null) {
            generator.writeString(DateTimeUtils.DATE_TIME_FORMATTER_TIMESTAMP_WITH_MILLIS_AND_OFFSET.format(value));
        } else {
            generator.writeNull();
        }
    }

   
}
