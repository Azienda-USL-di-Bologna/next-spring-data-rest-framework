package it.nextsw.common.interceptors;

import it.nextsw.common.controller.HibernateEntityInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 *
 * @author gusgus
 */
public class RequestInterceptor implements AsyncHandlerInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestInterceptor.class);
    @Override
    public void afterCompletion(HttpServletRequest hsr, HttpServletResponse hsr1, Object o, Exception excptn) throws Exception {
        // Svuoto il threadLocal della query per il ranking
        HibernateEntityInterceptor.rankQueryObj.remove();
    }
}
