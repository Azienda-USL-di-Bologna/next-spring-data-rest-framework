
package it.nextsw.common.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class MyInterceptorRegistration implements HibernatePropertiesCustomizer {

    @Autowired
    private HibernateEntityInterceptor myInterceptor;

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("hibernate.session_factory.interceptor", myInterceptor);
    }
}
