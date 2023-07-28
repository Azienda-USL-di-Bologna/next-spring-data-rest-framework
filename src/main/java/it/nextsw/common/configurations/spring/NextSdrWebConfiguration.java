package it.nextsw.common.configurations.spring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.nextsw.common.configurations.jackson.AbstractJsonTypeConverterFactory;
import it.nextsw.common.interceptors.RequestInterceptor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *
 * @author gusgus
 */
@Configuration
public class NextSdrWebConfiguration implements WebMvcConfigurer {

    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestInterceptor());
    }

    /**
     * viengono aggiunti i convertitori personalizzati che usa spring
     * @param formatterRegistry 
     */
    @Override
    public void addFormatters(FormatterRegistry formatterRegistry) {
        // aggiunge il convertitore che permette di convertire il json passato nella query string quando si vuole effettuare un filtro
        formatterRegistry.addConverterFactory(new AbstractJsonTypeConverterFactory(objectMapper));
    }

}
