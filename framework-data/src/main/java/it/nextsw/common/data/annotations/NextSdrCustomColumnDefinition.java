package it.nextsw.common.data.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Questa è l'annotazione che deve essere usata sui campi che necessitato di un
 * riconoscimento particolare per il binding. Ad esempio i campi bit.
 * {@link NextSdrQueryDslRepository}
 *
 * @author gus
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface NextSdrCustomColumnDefinition {

    /**
     * Definisce il nome della relazione dell'ancestor
     *
     * @return
     */
    public String name();
}
