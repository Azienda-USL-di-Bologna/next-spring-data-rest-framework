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
public interface RestControllerInterceptor {

    public final String BEFORE_SELECT_QUERY_INTERCEPTOR_METHOD_NAME = "beforeSelectQueryInterceptor";
    public final String AFTER_SELECT_QUERY_INTERCEPTOR_METHOD_NAME = "afterSelectQueryInterceptor";
    
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request);

    // lista di risultati estratti da modificare
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request);

    // elemento da modificare di ritorno dalla query 
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request);

    public Object beforeCreateInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException;

    public Object beforeUpdateInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException;

    public void beforeDeleteInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException;
}
