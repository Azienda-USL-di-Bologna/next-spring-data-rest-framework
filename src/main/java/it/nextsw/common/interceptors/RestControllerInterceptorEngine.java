package it.nextsw.common.interceptors;

import com.querydsl.core.types.Predicate;
import it.nextsw.common.annotations.Interceptor;
import it.nextsw.common.interceptors.exceptions.InterceptorException;
import it.nextsw.common.interceptors.exceptions.RollBackInterceptorException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class RestControllerInterceptorEngine {

    @Value(value = "${common.configuration.interceptors-package:it.bologna.ausl.shalbo.interceptors}")
    private String interceptorsPackage;

    @Autowired
    private ListableBeanFactory beanFactory;

    private static final Map<String, List<RestControllerInterceptor>> INTERCEPTORS = new ConcurrentHashMap<>();

    public Predicate executeBeforeSelectQueryInterceptor(Predicate initialPredicate, Class entityClass, HttpServletRequest request, Map<String, String> additionalData) throws ClassNotFoundException {
        fillInterceptorsCache();
        List<RestControllerInterceptor> interceptors = INTERCEPTORS.get(entityClass.getName());
        if (interceptors != null) {
            for (RestControllerInterceptor interceptor : interceptors) {
                initialPredicate = interceptor.beforeSelectQueryInterceptor(initialPredicate, additionalData, request);
            }
        }
        return initialPredicate;
    }

    public Object executeAfterSelectQueryInterceptor(Object entity, List<Object> entities, Class entityClass, HttpServletRequest request, Map<String, String> additionalData) throws ClassNotFoundException, InterceptorException {

        Object res = null;

        if (entity != null) {
            res = entity;
        } else if (entities != null) {
            res = entities;
        } else {
            throw new InterceptorException("errore, sia entity che entities sono nulli, passane almeno uno");
        }

        fillInterceptorsCache();
        List<RestControllerInterceptor> interceptors = INTERCEPTORS.get(entityClass.getName());
        if (interceptors != null) {
            for (RestControllerInterceptor interceptor : interceptors) {
                if (entity != null) {
                    res = interceptor.afterSelectQueryInterceptor(entity, additionalData, request);
                } else {
                    res = interceptor.afterSelectQueryInterceptor(entities, additionalData, request);
                }
            }
        }
        return res;
    }

    public Object executebeforeCreateInterceptor(Object entity, HttpServletRequest request, Map<String, String> additionalData) throws ClassNotFoundException, RollBackInterceptorException {
        fillInterceptorsCache();
        List<RestControllerInterceptor> interceptors = INTERCEPTORS.get(entity.getClass().getName());
        if (interceptors != null) {
            for (RestControllerInterceptor interceptor : interceptors) {
                entity = interceptor.beforeCreateInterceptor(entity, additionalData, request);
            }
        }
        return entity;
    }

    public Object executebeforeUpdateInterceptor(Object entity, HttpServletRequest request, Map<String, String> additionalData) throws ClassNotFoundException, RollBackInterceptorException {
        fillInterceptorsCache();
        List<RestControllerInterceptor> interceptors = INTERCEPTORS.get(entity.getClass().getName());
        if (interceptors != null) {
            for (RestControllerInterceptor interceptor : interceptors) {
                entity = interceptor.beforeUpdateInterceptor(entity, additionalData, request);
            }
        }
        return entity;
    }

    public Object executebeforeDeleteInterceptor(Object entity, HttpServletRequest request, Map<String, String> additionalData) throws ClassNotFoundException, RollBackInterceptorException {
        fillInterceptorsCache();
        List<RestControllerInterceptor> interceptors = INTERCEPTORS.get(entity.getClass().getName());
        if (interceptors != null) {
            for (RestControllerInterceptor interceptor : interceptors) {
                interceptor.beforeDeleteInterceptor(entity, additionalData, request);
            }
        }
        return entity;
    }

    private void fillInterceptorsCache() throws ClassNotFoundException {
        if (INTERCEPTORS == null || INTERCEPTORS.isEmpty()) {
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Interceptor.class));
            for (BeanDefinition bd : scanner.findCandidateComponents(interceptorsPackage)) {
                Object interceptorFound = beanFactory.getBean(Class.forName(bd.getBeanClassName()));
                Interceptor annotation = interceptorFound.getClass().getAnnotation(Interceptor.class);
                Class target = annotation.target();
//                System.out.println("Trovato: " + bd.getBeanClassName() + " " + target + " " + annotation.name());
                List<RestControllerInterceptor> interceptors = INTERCEPTORS.get(target.getName());
                if (interceptors == null) {
                    interceptors = new ArrayList<>();
                }
                interceptors.add((RestControllerInterceptor) interceptorFound);
                INTERCEPTORS.put(target.getName(), interceptors);
            }
        }
    }
}
