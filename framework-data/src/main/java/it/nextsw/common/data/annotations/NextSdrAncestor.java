package it.nextsw.common.data.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Questa è l'annotazione che deve essere usata sui campi di due o più entità 
 * discendenti se sono fk che puntano alla stessa riga di un'altra entità.
 * {@link NextSdrQueryDslRepository}
 *
 * @author gus&gdm
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface NextSdrAncestor {

    /**
     * Definisce il nome della relazione dell'ancestor
     *
     * @return
     */
    public String relationName();
}
