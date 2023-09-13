package it.nextsw.common.utils;


import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

/**
 * Questa classe permette di reperire il context applicativo tramite il quale si posso poi reperire i Bean
 * Utile in classi non Component per poter reperire i Bean
 * @author gdm
 */
@Service
public class ApplicationContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        ApplicationContextUtil.applicationContext = context;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

}