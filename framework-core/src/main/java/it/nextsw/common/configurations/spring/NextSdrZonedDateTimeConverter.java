package it.nextsw.common.configurations.spring;

  import java.time.LocalDate;
import java.time.LocalDateTime;
    import java.time.ZoneId;
    import java.time.ZonedDateTime;
    import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
    import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
    import org.springframework.core.convert.converter.Converter;
    import org.springframework.stereotype.Component;
    
    
@Component
public class NextSdrZonedDateTimeConverter implements Converter<String, ZonedDateTime> {
private static final Logger log = LoggerFactory.getLogger(NextSdrZonedDateTimeConverter.class);
    
    @Override
    public ZonedDateTime convert(String source) {
        try {
            if (source == null) {
                return null;
            }
            if (!source.contains(":")) {
                LocalDate ld = LocalDate.parse(source, DateTimeFormatter.ISO_DATE);
                ZonedDateTime atStartOfDay = ld.atStartOfDay(ZoneId.systemDefault());
                return atStartOfDay;
            } else {
                ZonedDateTime dateTime = ZonedDateTime.parse(source, DateTimeFormatter.ISO_ZONED_DATE_TIME);
                return dateTime;
            }
        } catch (Exception ex) {
            try {
                return LocalDateTime.parse(source, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(ZoneId.systemDefault());
            } catch (Exception subEx) {
                log.error(String.format("errore nell conversione della data %s", source), subEx);
                throw subEx;
            }
        }
    }

}