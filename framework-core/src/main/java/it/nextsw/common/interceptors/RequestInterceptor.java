package it.nextsw.common.interceptors;

import it.nextsw.common.controller.HibernateEntityInspector;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 *
 * @author gusgus
 */
public class RequestInterceptor implements AsyncHandlerInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestInterceptor.class);
    
    
    
//    @Override
//    public void afterCompletion(HttpServletRequest hsr, HttpServletResponse hsr1, Object o, Exception excptn) throws Exception {
//        // Svuoto il threadLocal della query per il ranking
//        HibernateEntityInterceptor.rankQueryObj.remove();
//    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
       // Svuoto il threadLocal della query per il ranking
       HibernateEntityInspector.rankQueryObj.remove();
       AsyncHandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
