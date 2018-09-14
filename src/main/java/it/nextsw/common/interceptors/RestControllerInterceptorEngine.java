package it.nextsw.common.interceptors;

import com.querydsl.core.types.Predicate;
import it.nextsw.common.interceptors.exceptions.InterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import it.nextsw.common.utils.EntityReflectionUtils;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

/**
 * Questa è la classe registro che si occupa di smistare le richieste agli {@link NextSdrControllerInterceptor}
 * adeguati, ovvero che dichiarano come {@link NextSdrControllerInterceptor#getTargetEntityClass()} la classe della richiesta
 */
@Component
public class RestControllerInterceptorEngine {
    
    private static final Logger log = LoggerFactory.getLogger(RestControllerInterceptorEngine.class);

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
    protected Map<String, List<NextSdrControllerInterceptor>> interceptorsMap;
    

    public Predicate executeBeforeSelectQueryInterceptor(Predicate initialPredicate, Class entityClass, HttpServletRequest request, Map<String, String> additionalData) throws ClassNotFoundException, EntityReflectionException {
//        fillInterceptorsCache();
        List<NextSdrControllerInterceptor> interceptors = getInterceptors(entityReflectionUtils.getEntityFromProxyClass(entityClass));
        if (interceptors != null) {
            for (NextSdrControllerInterceptor interceptor : interceptors) {
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
        log.info(String.format("find %s interceptors on %s...", "afterSelectQueryInterceptor", res.toString()));
        List<NextSdrControllerInterceptor> interceptors = getInterceptors(entityReflectionUtils.getEntityFromProxyClass(entityClass));
        if (interceptors != null) {
            for (NextSdrControllerInterceptor interceptor : interceptors) {
                if (entity != null) {
                    log.info(String.format("execute %s on %s", "afterSelectQueryInterceptor", entity.toString()));
                    res = interceptor.afterSelectQueryInterceptor(entity, additionalData, request);
                } else {
                    log.info(String.format("execute %s on %s", "afterSelectQueryInterceptor", entities.toString()));
                    res = interceptor.afterSelectQueryInterceptor(entities, additionalData, request);
                }
            }
        }
        return res;
    }

    public Object executebeforeCreateInterceptor(Object entity, HttpServletRequest request, Map<String, String> additionalData) throws ClassNotFoundException, AbortSaveInterceptorException, EntityReflectionException {
//        fillInterceptorsCache();
        log.info(String.format("find %s interceptors on %s...", "beforeCreateEntityInterceptor", entity.toString()));
        List<NextSdrControllerInterceptor> interceptors = getInterceptors(entityReflectionUtils.getEntityFromProxyObject(entity));
        if (interceptors != null) {
            for (NextSdrControllerInterceptor interceptor : interceptors) {
                log.info(String.format("execute %s on %s", "beforeCreateEntityInterceptor", entity.toString()));
                entity = interceptor.beforeCreateEntityInterceptor(entity, additionalData, request);
            }
        }
        return entity;
    }

    public Object executebeforeUpdateInterceptor(Object entity, HttpServletRequest request, Map<String, String> additionalData) throws ClassNotFoundException, AbortSaveInterceptorException, EntityReflectionException {
        log.info(String.format("find %s interceptors on %s...", "beforeUpdateEntityInterceptor", entity.toString()));
        List<NextSdrControllerInterceptor> interceptors = getInterceptors(entityReflectionUtils.getEntityFromProxyObject(entity));
        if (interceptors != null) {
            for (NextSdrControllerInterceptor interceptor : interceptors) {
                log.info(String.format("execute %s on %s", "beforeUpdateEntityInterceptor", entity.toString()));
                entity = interceptor.beforeUpdateEntityInterceptor(entity, additionalData, request);
            }
        }
        return entity;
    }

    public Object executebeforeDeleteInterceptor(Object entity, HttpServletRequest request, Map<String, String> additionalData) throws ClassNotFoundException, AbortSaveInterceptorException, EntityReflectionException, SkipDeleteInterceptorException {
        log.info(String.format("find %s interceptors on %s...", "beforeDeleteEntityInterceptor", entity.toString()));
        List<NextSdrControllerInterceptor> interceptors = getInterceptors(entityReflectionUtils.getEntityFromProxyObject(entity));
        if (interceptors != null) {
            for (NextSdrControllerInterceptor interceptor : interceptors) {
                log.info(String.format("execute %s on %s", "beforeDeleteEntityInterceptor", entity.toString()));
                interceptor.beforeDeleteEntityInterceptor(entity, additionalData, request);
            }
        }
        return entity;
    }

    public List<NextSdrControllerInterceptor> getInterceptors(Class entityClass) {
        return interceptorsMap.get(entityClass.getName());
    }

    /**
     * torna "true" se per la classe Entità passata è implementato almeno un beforeSelectQueryInterceptor
     * @param entityClass
     * @return 
     */
    public boolean isImplementedBeforeQueryInterceptor(Class entityClass) {
        List<NextSdrControllerInterceptor> interceptors = interceptorsMap.get(entityClass.getName());
        return interceptors != null && interceptors.stream().anyMatch((interceptor) -> (Arrays.stream(interceptor.getClass().getDeclaredMethods()).anyMatch(method -> method.getName().equals(NextSdrControllerInterceptor.BEFORE_SELECT_QUERY_INTERCEPTOR_METHOD_NAME))));
    }
}
