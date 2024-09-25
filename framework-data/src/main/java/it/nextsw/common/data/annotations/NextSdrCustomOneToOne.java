package it.nextsw.common.data.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author gdm
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface NextSdrCustomOneToOne {

    /**
     * Da usare nelle entità derivanti da viste, quando il campo FK è singolo e non lista.
     * Definisce il nome del capo da usare nella where condition nella query sull'entità target
     *
     * @return
     */
    public String mappedBy();
}
