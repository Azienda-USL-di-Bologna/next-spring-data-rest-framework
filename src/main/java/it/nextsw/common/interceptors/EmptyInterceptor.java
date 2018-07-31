package it.nextsw.common.interceptors;

import com.querydsl.core.types.Predicate;
import it.nextsw.common.interceptors.exceptions.RollBackInterceptorException;
import java.util.Collection;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author gdm
 */
public abstract class EmptyInterceptor implements RestControllerInterceptor {

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
    public Object beforeCreateInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException {
        return entity;
    }

    @Override
    public Object beforeUpdateInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException {
        return entity;
    }

    @Override
    public void beforeDeleteInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException {
    }
}
