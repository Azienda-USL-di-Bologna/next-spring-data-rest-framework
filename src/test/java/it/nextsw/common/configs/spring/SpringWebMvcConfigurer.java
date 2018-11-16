package it.nextsw.common.configs.spring;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.List;

@Configuration
@EnableSpringDataWebSupport
public class SpringWebMvcConfigurer extends HateoasAwareSpringDataWebConfiguration {


    public SpringWebMvcConfigurer(ApplicationContext context, ObjectFactory<ConversionService> conversionService) {
        super(context, conversionService);
    }

//    @Override
//    public void addArgumentResolvers(
//            List<HandlerMethodArgumentResolver> argumentResolvers) {
//        List<HandlerMethodArgumentResolver> customArgumentResolvers = repositoryExporterHandlerAdapter.getCustomArgumentResolvers();
//        argumentResolvers.addAll(customArgumentResolvers);
//    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        super.addArgumentResolvers(argumentResolvers);
    }



}
