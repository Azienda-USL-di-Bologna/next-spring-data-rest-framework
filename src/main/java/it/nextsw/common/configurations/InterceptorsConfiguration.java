package it.nextsw.common.configurations;

import it.nextsw.common.annotations.Interceptor;
import it.nextsw.common.interceptors.RestControllerInterceptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 *
 * @author gdm
 */
@Configuration
public class InterceptorsConfiguration {
    private static final Logger log = LoggerFactory.getLogger(InterceptorsConfiguration.class);

    @Value(value = "${common.configuration.interceptors-package:it.bologna.ausl.shalbo.interceptors}")
    private String interceptorsPackage;

    @Autowired
    private ListableBeanFactory beanFactory;

    @Bean(name = "interceptorsMap")
    public Map<String, List<RestControllerInterceptor>> interceptorsMap() throws ClassNotFoundException, IOException {
        Map<String, List<RestControllerInterceptor>> interceptorsMap = new HashMap();

        Map<String, Object> interceptorBeansMap = beanFactory.getBeansWithAnnotation(it.nextsw.common.annotations.Interceptor.class);
        Collection<Object> interceptorBeans = interceptorBeansMap.values();
        for (Object interceptorBean: interceptorBeans) {
            Interceptor annotation = interceptorBean.getClass().getAnnotation(Interceptor.class);
            Class target = annotation.target();
                List<RestControllerInterceptor> interceptors = interceptorsMap.get(target.getName());
                if (interceptors == null) {
                    interceptors = new ArrayList<>();
                }
                interceptors.add((RestControllerInterceptor) interceptorBean);
                interceptorsMap.put(target.getName(), interceptors);
        }
        
//            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
//            scanner.addIncludeFilter(new AnnotationTypeFilter(Interceptor.class));
//            for (BeanDefinition bd : scanner.findCandidateComponents(interceptorsPackage)) {
//                Object interceptorFound = beanFactory.getBean(Class.forName(bd.getBeanClassName()));
//                Interceptor annotation = interceptorFound.getClass().getAnnotation(Interceptor.class);
//                Class target = annotation.target();
//                List<RestControllerInterceptor> interceptors = interceptorsMap.get(target.getName());
//                if (interceptors == null) {
//                    interceptors = new ArrayList<>();
//                }
//                interceptors.add((RestControllerInterceptor) interceptorFound);
//                interceptorsMap.put(target.getName(), interceptors);
//            }
        return interceptorsMap;
    }
}
