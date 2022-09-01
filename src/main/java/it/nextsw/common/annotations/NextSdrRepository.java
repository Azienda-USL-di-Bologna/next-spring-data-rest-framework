package it.nextsw.common.annotations;

import it.nextsw.common.repositories.NextSdrQueryDslRepository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Questa è l'annotazione che deve essere usata sui repository da utilizzare con
 * questo framework. Questi repository DEVONO estendere l'interfaccia
 * {@link NextSdrQueryDslRepository}
 *
 * @author spritz
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface NextSdrRepository {

    /**
     * Definisce il path su cui risponde questo repository
     *
     * @return
     */
    public String repositoryPath();

    /**
     * Definisce la projection di default che il repository utilizzerà; questa
     * viene ignorata se la projection viene passata come parametro nella
     * richiesta
     *
     * @return
     */
    public Class defaultProjection();
}
