package it.nextsw.common.interceptors;

import com.google.common.collect.Streams;
import com.querydsl.core.types.Predicate;
import it.nextsw.common.interceptors.exceptions.InterceptorException;
import it.nextsw.common.interceptors.exceptions.RollBackInterceptorException;
import it.nextsw.common.utils.EntityReflectionUtils;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class RestControllerInterceptorEngine {

//    @Value(value = "${common.configuration.interceptors-package:it.bologna.ausl.shalbo.interceptors}")
//    private String interceptorsPackage;
//
//    @Autowired
//    private ListableBeanFactory beanFactory;
//    
    @Autowired
    private EntityReflectionUtils entityReflectionUtils;
    

    @Autowired
    @Qualifier(value = "interceptorsMap")
    protected Map<String, List<RestControllerInterceptor>> interceptorsMap;
    
//    private static final Map<String, List<RestControllerInterceptor>> INTERCEPTORS = new ConcurrentHashMap<>();

    public Predicate executeBeforeSelectQueryInterceptor(Predicate initialPredicate, Class entityClass, HttpServletRequest request, Map<String, String> additionalData) throws ClassNotFoundException, EntityReflectionException {
//        fillInterceptorsCache();
        List<RestControllerInterceptor> interceptors = getInterceptors(entityReflectionUtils.getEntityFromProxyClass(entityClass));
        if (interceptors != null) {
            for (RestControllerInterceptor interceptor : interceptors) {
                initialPredicate = interceptor.beforeSelectQueryInterceptor(initialPredicate, additionalData, request);
            }
        }
        return initialPredicate;
    }

    public Object executeAfterSelectQueryInterceptor(Object entity, Collection<Object> entities, Class entityClass, HttpServletRequest request, Map<String, String> additionalData) throws ClassNotFoundException, InterceptorException, EntityReflectionException {

        Object res = null;

        if (entity != null) {
            res = entity;
        } else if (entities != null) {
            res = entities;
        } else {
            throw new InterceptorException("errore, sia entity che entities sono nulli, passane almeno uno");
        }

//        fillInterceptorsCache();
        List<RestControllerInterceptor> interceptors = getInterceptors(entityReflectionUtils.getEntityFromProxyClass(entityClass));
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

    public Object executebeforeCreateInterceptor(Object entity, HttpServletRequest request, Map<String, String> additionalData) throws ClassNotFoundException, RollBackInterceptorException, EntityReflectionException {
//        fillInterceptorsCache();
        List<RestControllerInterceptor> interceptors = getInterceptors(entityReflectionUtils.getEntityFromProxyObject(entity));
        if (interceptors != null) {
            for (RestControllerInterceptor interceptor : interceptors) {
                entity = interceptor.beforeCreateInterceptor(entity, additionalData, request);
            }
        }
        return entity;
    }

    public Object executebeforeUpdateInterceptor(Object entity, HttpServletRequest request, Map<String, String> additionalData) throws ClassNotFoundException, RollBackInterceptorException, EntityReflectionException {
//        fillInterceptorsCache();
        List<RestControllerInterceptor> interceptors = getInterceptors(entityReflectionUtils.getEntityFromProxyObject(entity));
        if (interceptors != null) {
            for (RestControllerInterceptor interceptor : interceptors) {
                entity = interceptor.beforeUpdateInterceptor(entity, additionalData, request);
            }
        }
        return entity;
    }

    public Object executebeforeDeleteInterceptor(Object entity, HttpServletRequest request, Map<String, String> additionalData) throws ClassNotFoundException, RollBackInterceptorException, EntityReflectionException {
//        fillInterceptorsCache();
        List<RestControllerInterceptor> interceptors = getInterceptors(entityReflectionUtils.getEntityFromProxyObject(entity));
        if (interceptors != null) {
            for (RestControllerInterceptor interceptor : interceptors) {
                interceptor.beforeDeleteInterceptor(entity, additionalData, request);
            }
        }
        return entity;
    }

    public List<RestControllerInterceptor> getInterceptors(Class entityClass) {
        return interceptorsMap.get(entityClass.getName());
    }

    /**
     * torna "true" se per la classe Entità passata è implementato almeno un beforeSelectQueryInterceptor
     * @param entityClass
     * @return 
     */
    public boolean isImplementedBeforeQueryInterceptor(Class entityClass) {
        List<RestControllerInterceptor> interceptors = interceptorsMap.get(entityClass.getName());
        return interceptors != null && interceptors.stream().anyMatch((interceptor) -> (Arrays.stream(interceptor.getClass().getDeclaredMethods()).anyMatch(method -> method.getName().equals(RestControllerInterceptor.BEFORE_SELECT_QUERY_INTERCEPTOR_METHOD_NAME))));
    }
}
