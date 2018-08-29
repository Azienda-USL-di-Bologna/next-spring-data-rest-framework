package it.nextsw.common.configurations;

import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.NextSdrControllerInterceptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

/**
 * La classe s
 *
 * @author gdm
 */
@Configuration
public class NextSdrInterceptorsConfiguration {
    private static final Logger log = LoggerFactory.getLogger(NextSdrInterceptorsConfiguration.class);

    private static final int ORDER_DEFAULT_VALUE = Integer.MAX_VALUE;

    @Value(value = "${common.configuration.interceptors-package:it.bologna.ausl.shalbo.interceptors}")
    private String interceptorsPackage;

    @Autowired
    private ListableBeanFactory beanFactory;

    @Autowired(required = false)
    private List<NextSdrControllerInterceptor> interceptors;


    @Bean(name = "interceptorsMap")
    public Map<String, List<NextSdrControllerInterceptor>> interceptorsMap() throws ClassNotFoundException, IOException {
        Map<String, List<NextSdrControllerInterceptor>> interceptorsMap = new HashMap();

        if (StringUtils.hasText(interceptorsPackage)) {
            // Map<String, Object> interceptorBeansMap = beanFactory.getBean(it.nextsw.common.annotations.NextSdrInterceptor.class);
            // Collection<Object> interceptorBeans = interceptorBeansMap.values();
            for (NextSdrControllerInterceptor interceptorBean : interceptors) {
                NextSdrInterceptor annotation = interceptorBean.getClass().getAnnotation(NextSdrInterceptor.class);
                Order order= interceptorBean.getClass().getAnnotation(Order.class);
                int interceptorOrder = order!=null ? order.value() : ORDER_DEFAULT_VALUE;
                Class target = interceptorBean.getTargetEntityClass();

                List<NextSdrControllerInterceptor> interceptors = interceptorsMap.get(target.getName());
                if (interceptors == null) {
                    interceptors = new ArrayList<>();
                }
                interceptors.add((NextSdrControllerInterceptor) interceptorBean);
                // ordino la lista
                interceptorsMap.put(target.getName(), interceptors);
                interceptors.sort((interceptor1, interceptor2) -> {
                    Order order1= interceptor1.getClass().getAnnotation(Order.class);
                    int interceptor1Order = order1!=null ? order1.value() : ORDER_DEFAULT_VALUE;
                    Order order2= interceptor2.getClass().getAnnotation(Order.class);
                    int interceptor2Order = order2!=null ? order2.value() : ORDER_DEFAULT_VALUE;
                    return interceptor1Order > interceptor2Order ? 1 : -1 ;
                });
            }
        }


//            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
//            scanner.addIncludeFilter(new AnnotationTypeFilter(NextSdrInterceptor.class));
//            for (BeanDefinition bd : scanner.findCandidateComponents(interceptorsPackage)) {
//                Object interceptorFound = beanFactory.getBean(Class.forName(bd.getBeanClassName()));
//                NextSdrInterceptor annotation = interceptorFound.getClass().getAnnotation(NextSdrInterceptor.class);
//                Class target = annotation.target();
//                List<NextSdrControllerInterceptor> interceptors = interceptorsMap.get(target.getName());
//                if (interceptors == null) {
//                    interceptors = new ArrayList<>();
//                }
//                interceptors.add((NextSdrControllerInterceptor) interceptorFound);
//                interceptorsMap.put(target.getName(), interceptors);
//            }
        return interceptorsMap;
    }
}
