package it.nextsw.common.configurations;

import it.nextsw.common.interceptors.NextSdrControllerInterceptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * La classe s
 *
 * @author gdm
 */
@Configuration
public class NextSdrInterceptorsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NextSdrInterceptorsConfiguration.class);

    private static final int ORDER_DEFAULT_VALUE = Integer.MAX_VALUE;

    @Autowired(required = false)
    private List<NextSdrControllerInterceptor> interceptors;

    @Bean(name = "interceptorsMap")
    public Map<String, List<NextSdrControllerInterceptor>> interceptorsMap() throws ClassNotFoundException, IOException {
        Map<String, List<NextSdrControllerInterceptor>> interceptorsMap = new HashMap();

        if (interceptors != null && !interceptors.isEmpty()) {
            for (NextSdrControllerInterceptor interceptorBean : interceptors) {
                Class target = interceptorBean.getTargetEntityClass();

                List<NextSdrControllerInterceptor> entityInterceptors = interceptorsMap.get(target.getName());
                if (entityInterceptors == null) {
                    entityInterceptors = new ArrayList<>();
                }
                entityInterceptors.add((NextSdrControllerInterceptor) interceptorBean);
                // ordino la lista
                interceptorsMap.put(target.getName(), entityInterceptors);
                entityInterceptors.sort((interceptor1, interceptor2) -> {
                    Order order1 = interceptor1.getClass().getAnnotation(Order.class);
                    int interceptor1Order = order1 != null ? order1.value() : ORDER_DEFAULT_VALUE;
                    Order order2 = interceptor2.getClass().getAnnotation(Order.class);
                    int interceptor2Order = order2 != null ? order2.value() : ORDER_DEFAULT_VALUE;
                    return interceptor1Order > interceptor2Order ? 1 : -1;
                });
            }
        }
        return interceptorsMap;
    }
}
