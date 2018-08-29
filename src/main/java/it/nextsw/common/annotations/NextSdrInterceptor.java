package it.nextsw.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *Classe interceptor che permette di specificare informazioni aggiuntive sull'interceptor
 * @author gdm
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NextSdrInterceptor {

    /**
     * Permette di specificare il nome dell'interceptor
     * Non Ancora utilizzato
     * @return
     */
    public String name();
}
