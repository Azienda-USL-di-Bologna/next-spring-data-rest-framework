package it.nextsw.common.interceptors;

import com.querydsl.core.types.Predicate;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import java.util.Collection;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * Un {@link NextSdrControllerInterceptor} con alcuni metodi implementati di default
 *
 * @author gdm
 */
public abstract class NextSdrEmptyControllerInterceptor implements NextSdrControllerInterceptor {

    @Override
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request) {
        return initialPredicate;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request) {
        return entities;
    }

    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) {
        return entity;
    }

    @Override
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortSaveInterceptorException {
        return entity;
    }

    @Override
    public Object beforeUpdateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortSaveInterceptorException {
        return entity;
    }

    @Override
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws AbortSaveInterceptorException, SkipDeleteInterceptorException {
    }
}
