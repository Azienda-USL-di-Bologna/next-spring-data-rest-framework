package it.nextsw.common.data.configurations;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import it.nextsw.common.data.types.AbstractJsonType;
import it.nextsw.common.data.types.AbstractJsonTypeForQueryDslExecutor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;
/**
 * 
 * @author gdm
 * 
 * Questa è la factory da aggiungere al formatter regisrty che permette di convertire il json passato nella query string quando si vuole effettuare un filtro
 * su un campo json.
 * 
 * Viene creato l'oggetto AbstractJsonTypeForQueryDslExecutor che rappresenta il json
 */
@Component
public class AbstractJsonTypeConverterFactory implements ConverterFactory<String, AbstractJsonType> {

    private final ObjectMapper objectMapper;
    
    public AbstractJsonTypeConverterFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T extends AbstractJsonType> Converter<String, T> getConverter(Class<T> targetClass) {
        return new StringToAbstractJsonTypeConverter<>(objectMapper);
    }

    private static class StringToAbstractJsonTypeConverter<T extends AbstractJsonType> implements Converter<String, T> {

        private final ObjectMapper mapper;

        public StringToAbstractJsonTypeConverter(ObjectMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public T convert(String source) {
            JsonNode readValue;
            readValue = mapper.readValue((String)source, JsonNode.class);
            return (T) new AbstractJsonTypeForQueryDslExecutor(readValue);
            
        }
    }
}