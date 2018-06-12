package it.nextsw.common.interceptors;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import it.nextsw.common.interceptors.exceptions.RollBackInterceptorException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author gdm
 */
public interface RestControllerInterceptor {
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request);
    
    public List<Object> afterSelectQueryInterceptor(List<Object> entities, Map<String, String> additionalData, HttpServletRequest request);
    
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request);
    
    public Object beforeCreateInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException;

    public Object beforeUpdateInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException;
    
    public void beforeDeleteInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request) throws RollBackInterceptorException;
}
