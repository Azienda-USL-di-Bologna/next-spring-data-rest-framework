package it.nextsw.common.interceptors;

import com.querydsl.core.types.Predicate;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;

import java.util.Collection;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 *  L'interfaccia di base per gli interceptor, tutti gli interceptor devono implementarla
 *  Supporta l'annotazione {@link NextSdrInterceptor} e
 *  {@link org.springframework.core.annotation.Order} per definire l'ordine con cui devono essere eseguiti
 */
public interface NextSdrControllerInterceptor {

    public final String BEFORE_SELECT_QUERY_INTERCEPTOR_METHOD_NAME = "beforeSelectQueryInterceptor";
    public final String AFTER_SELECT_QUERY_INTERCEPTOR_METHOD_NAME = "afterSelectQueryInterceptor";

    public enum InterceptorType {BEFORE,AFTER};

    public enum InterceptorOperation {QUERY,CREATE,UPDATE,DELETE};

    /**
     * Definisce la classe su cui lavora l'interceptor
     * @return
     */
    public Class getTargetEntityClass();

    /**
     * Questo metodo viene eseguito prima di eseguire l'eventuale query di select
     * @param initialPredicate il predicato così come arriva al webservices
     * @param additionalData parametri aggiuntivi che possono essere inviati al webservices
     * @param request la request della chiamata
     * @param mainEntity indica se l'interceptor è eseguito sull'entità principale su cui è stata fatta la richiesta o
     *                   su una entità che compare come correlata
     * @param projectionClass la classe relativa alla projection utilizzata per il reperimento dei dell'entità
     * @return il predicato da utilizzare per eseguire la query
     * @throws AbortLoadInterceptorException se viene lanciata questa eccezione la transazione attuale va in rollback
     */
    public Predicate beforeSelectQueryInterceptor(Predicate initialPredicate, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException;

    /**
     * Questo metodo viene eseguito subito dopo la query nel caso la query torni più di un risultato.In questo metodo è possibile fare un post filtraggio sui risultati
     * @param entities la lista di entità risultante dalla query eseguita
     * @param additionalData parametri aggiuntivi che possono essere inviati al webservices
     * @param request la request della chiamata
     * @param mainEntity indica se l'interceptor è eseguito sull'entità principale su cui è stata fatta la richiesta o
     *                   su una entità che compare come correlata
     * @param projectionClass la classe relativa alla projection utilizzata per il reperimento dei dell'entità
     * @return la collection di oggetti che verrano ritornati al richiedente
     * @throws AbortLoadInterceptorException se viene lanciata questa eccezione la transazione attuale va in rollback
     */
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException;

    /**
     * Questo metodo viene eseguito subito dopo la query nel caso la query torni un risultato.In questo metodo è possibile fare un post filtraggio sui risultati
     * @param entity l' entità risultante dalla query eseguita
     * @param additionalData parametri aggiuntivi che possono essere inviati al webservices
     * @param request la request della chiamata
     * @param mainEntity indica se l'interceptor è eseguito sull'entità principale su cui è stata fatta la richiesta o
     *                   su una entità che compare come correlata
     * @param projectionClass la classe relativa alla projection utilizzata per il reperimento dei dell'entità
     * @return l'oggetto che verrà ritornato al richiedente
     * @throws AbortLoadInterceptorException se viene lanciata questa eccezione la transazione attuale va in rollback
     */
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException;

    /**
     * Questo metodo viene eseguito prima di una query di insert di un'entità
     * @param entity l'entità che sta per essere creata (inserita)
     * @param additionalData parametri aggiuntivi che possono essere inviati al webservices
     * @param request la request della chiamata
     * @param mainEntity indica se l'interceptor è eseguito sull'entità principale su cui è stata fatta la richiesta o
     *                   su una entità che compare come correlata
     * @param projectionClass la classe relativa alla projection indicata nella richiesta
     * @return l'oggetto con eventuali modifiche che verrà poi salvato
     * @throws AbortSaveInterceptorException se viene lanciata questa eccezione la transazione attuale va in rollback
     */
    public Object beforeCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException;

    /**
     * Questo metodo viene eseguito prima di una query di insert di un'entità
     * @param entity l'entità che sta per essere creata (inserita)
     * @param additionalData parametri aggiuntivi che possono essere inviati al webservices
     * @param request la request della chiamata
     * @param mainEntity indica se l'interceptor è eseguito sull'entità principale su cui è stata fatta la richiesta o
     *                   su una entità che compare come correlata
     * @param projectionClass la classe relativa alla projection indicata nella richiesta
     * @return l'oggetto con eventuali modifiche che verrà poi salvato
     * @throws AbortSaveInterceptorException se viene lanciata questa eccezione la transazione attuale va in rollback
     */
    public Object afterCreateEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException;


    /**
     * Questo metodo viene eseguito prima di una query di update di un'entità
     * @param entity l'entità che sta per essere aggiornata (update)
     * @param beforeUpdateEntity l'entità iniziale, senza che siano state fatte le modifiche passate nel body della richiesta
     * @param additionalData parametri aggiuntivi che possono essere inviati al webservices
     * @param request la request della chiamata
     * @param mainEntity indica se l'interceptor è eseguito sull'entità principale su cui è stata fatta la richiesta o
     *                   su una entità che compare come correlata
     * @param projectionClass la classe relativa alla projection indicata nella richiesta
     * @return l'oggetto con eventuali modifiche che verrà poi salvato
     * @throws AbortSaveInterceptorException se viene lanciata questa eccezione la transazione attuale va in rollback
     */
    public Object beforeUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException;

    /**
     * Questo metodo viene eseguito prima di una query di update di un'entità
     * @param entity l'entità che sta per essere aggiornata (update)
     * @param beforeUpdateEntity l'entità iniziale, senza che siano state fatte le modifiche passate nel body della richiesta
     * @param additionalData parametri aggiuntivi che possono essere inviati al webservices
     * @param request la request della chiamata
     * @param mainEntity indica se l'interceptor è eseguito sull'entità principale su cui è stata fatta la richiesta o
     *                   su una entità che compare come correlata
     * @param projectionClass la classe relativa alla projection indicata nella richiesta
     * @return l'oggetto con eventuali modifiche che verrà poi salvato
     * @throws AbortSaveInterceptorException se viene lanciata questa eccezione la transazione attuale va in rollback
     */
    public Object afterUpdateEntityInterceptor(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException;


    /**
     * Questo metodo viene eseguito prima di una query di delete di un'entità
     * @param entity l'entità che sta per essere cancellata (delete)
     * @param additionalData parametri aggiuntivi che possono essere inviati al webservices
     * @param request la request della chiamata
     * @param mainEntity indica se l'interceptor è eseguito sull'entità principale su cui è stata fatta la richiesta o
     *                   su una entità che compare come correlata
     * @param projectionClass la classe relativa alla projection indicata nella richiesta
     * @throws AbortSaveInterceptorException se viene lanciata questa eccezione la transazione attuale va in rollback
     * @throws it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException se viene lanciata questa eccezione la transazione prosegue, ma l'oggetto non viene eliminato
     */
    public void beforeDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException;

    /**
     * Questo metodo viene eseguito prima di una query di delete di un'entità
     * @param entity l'entità che sta per essere cancellata (delete)
     * @param additionalData parametri aggiuntivi che possono essere inviati al webservices
     * @param request la request della chiamata
     * @param mainEntity indica se l'interceptor è eseguito sull'entità principale su cui è stata fatta la richiesta o
     *                   su una entità che compare come correlata
     * @param projectionClass la classe relativa alla projection indicata nella richiesta
     * @throws AbortSaveInterceptorException se viene lanciata questa eccezione la transazione attuale va in rollback
     * @throws it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException se viene lanciata questa eccezione la transazione prosegue, ma l'oggetto non viene eliminato
     */
    public void afterDeleteEntityInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortSaveInterceptorException, SkipDeleteInterceptorException;

}
