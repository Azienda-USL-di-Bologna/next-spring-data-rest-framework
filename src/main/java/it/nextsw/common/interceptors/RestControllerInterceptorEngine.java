package it.nextsw.common.interceptors;

import com.querydsl.core.types.Predicate;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.InterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import it.nextsw.common.utils.EntityReflectionUtils;
import it.nextsw.common.utils.exceptions.EntityReflectionException;

import java.lang.reflect.*;
import java.util.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Questa è la classe registro che si occupa di smistare le richieste agli
 * {@link NextSdrControllerInterceptor} adeguati, ovvero che dichiarano come
 * {@link NextSdrControllerInterceptor#getTargetEntityClass()} la classe della
 * richiesta
 */
@Component
public class RestControllerInterceptorEngine {

    private static final Logger log = LoggerFactory.getLogger(RestControllerInterceptorEngine.class);

    @PersistenceContext
    EntityManager em;

    @Autowired
    @Qualifier(value = "interceptorsMap")
    protected Map<String, List<NextSdrControllerInterceptor>> interceptorsMap;

    public Predicate executeBeforeSelectQueryInterceptor(Predicate initialPredicate, Class entityClass, HttpServletRequest request, Map<String, String> additionalData, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException, ClassNotFoundException, EntityReflectionException {
//        fillInterceptorsCache();
        List<NextSdrControllerInterceptor> interceptors = getInterceptors(EntityReflectionUtils.getEntityFromProxyClass(entityClass));
        if (interceptors != null) {
            for (NextSdrControllerInterceptor interceptor : interceptors) {
                initialPredicate = interceptor.beforeSelectQueryInterceptor(initialPredicate, additionalData, request, mainEntity, projectionClass);
            }
        }
        return initialPredicate;
    }

    public Object executeAfterSelectQueryInterceptor(Object entity, Collection<Object> entities, Class entityClass, HttpServletRequest request, Map<String, String> additionalData, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException, ClassNotFoundException, InterceptorException, EntityReflectionException {

        Object res = null;

        if (entity != null) {
            res = entity;
        } else if (entities != null) {
            res = entities;
        } else {
            throw new InterceptorException("errore, sia entities che entities sono nulli, passane almeno uno");
        }

//        fillInterceptorsCache();
        log.info(String.format("find %s interceptors on %s...", "afterSelectQueryInterceptor", res.toString()));
        List<NextSdrControllerInterceptor> interceptors = getInterceptors(EntityReflectionUtils.getEntityFromProxyClass(entityClass));
        if (interceptors != null) {
            for (NextSdrControllerInterceptor interceptor : interceptors) {
                if (entity != null) {
                    log.info(String.format("execute %s on %s", "afterSelectQueryInterceptor", entity.toString()));
                    res = interceptor.afterSelectQueryInterceptor(entity, additionalData, request, mainEntity, projectionClass);
//                    em.detach(entities);
                } else {
                    log.info(String.format("execute %s on %s", "afterSelectQueryInterceptor", entities.toString()));
                    res = interceptor.afterSelectQueryInterceptor(entities, additionalData, request, mainEntity, projectionClass);
                    if (entities != null) {
                        for (Object e : entities) {
//                            em.detach(e);
                        }
                    }
                }
            }
        }

        return res;
    }

    public Object executebeforeCreateInterceptor(Object entity, HttpServletRequest request, Map<String, String> additionalData, boolean mainEntity, Class projectionClass) throws ClassNotFoundException, AbortSaveInterceptorException, EntityReflectionException {
//        fillInterceptorsCache();
        log.info(String.format("find %s interceptors on %s...", "beforeCreateEntityInterceptor", entity.toString()));
        List<NextSdrControllerInterceptor> interceptors = getInterceptors(EntityReflectionUtils.getEntityFromProxyObject(entity));
        if (interceptors != null) {
            for (NextSdrControllerInterceptor interceptor : interceptors) {
                log.info(String.format("execute %s on %s", "beforeCreateEntityInterceptor", entity.toString()));
                entity = interceptor.beforeCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass);
            }
        }
        return entity;
    }

    public Object executeafterCreateInterceptor(Object entity, HttpServletRequest request, Map<String, String> additionalData, boolean mainEntity, Class projectionClass) throws ClassNotFoundException, AbortSaveInterceptorException, EntityReflectionException {
//        fillInterceptorsCache();
        log.info(String.format("find %s interceptors on %s...", "afterCreateEntityInterceptor", entity.toString()));
        List<NextSdrControllerInterceptor> interceptors = getInterceptors(EntityReflectionUtils.getEntityFromProxyObject(entity));
        if (interceptors != null) {
            for (NextSdrControllerInterceptor interceptor : interceptors) {
                log.info(String.format("execute %s on %s", "afgerCreateEntityInterceptor", entity.toString()));
                entity = interceptor.afterCreateEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass);
            }
        }
        return entity;
    }

    public Object executebeforeUpdateInterceptor(Object entity, Object beforeUpdateEntity, HttpServletRequest request, Map<String, String> additionalData, boolean mainEntity, Class projectionClass) throws ClassNotFoundException, AbortSaveInterceptorException, EntityReflectionException {
        log.info(String.format("find %s interceptors on %s...", "beforeUpdateEntityInterceptor", entity.toString()));
        List<NextSdrControllerInterceptor> interceptors = getInterceptors(EntityReflectionUtils.getEntityFromProxyObject(entity));
        if (interceptors != null) {
            for (NextSdrControllerInterceptor interceptor : interceptors) {
                log.info(String.format("execute %s on %s", "beforeUpdateEntityInterceptor", entity.toString()));
                entity = interceptor.beforeUpdateEntityInterceptor(entity, beforeUpdateEntity, additionalData, request, mainEntity, projectionClass);
            }
        }
        return entity;
    }

    public Object executeafterUpdateInterceptor(Object entity, Object beforeUpdateEntity, HttpServletRequest request, Map<String, String> additionalData, boolean mainEntity, Class projectionClass) throws ClassNotFoundException, AbortSaveInterceptorException, EntityReflectionException {
        log.info(String.format("find %s interceptors on %s...", "afterUpdateEntityInterceptor", entity.toString()));
        List<NextSdrControllerInterceptor> interceptors = getInterceptors(EntityReflectionUtils.getEntityFromProxyObject(entity));
        if (interceptors != null) {
            for (NextSdrControllerInterceptor interceptor : interceptors) {
                log.info(String.format("execute %s on %s", "afterUpdateEntityInterceptor", entity.toString()));
                entity = interceptor.afterUpdateEntityInterceptor(entity, beforeUpdateEntity, additionalData, request, mainEntity, projectionClass);
            }
        }
        return entity;
    }

    public Object executebeforeDeleteInterceptor(Object entity, HttpServletRequest request, Map<String, String> additionalData, boolean mainEntity, Class projectionClass) throws ClassNotFoundException, AbortSaveInterceptorException, EntityReflectionException, SkipDeleteInterceptorException {
        log.info(String.format("find %s interceptors on %s...", "beforeDeleteEntityInterceptor", entity.toString()));
        List<NextSdrControllerInterceptor> interceptors = getInterceptors(EntityReflectionUtils.getEntityFromProxyObject(entity));
        if (interceptors != null) {
            for (NextSdrControllerInterceptor interceptor : interceptors) {
                log.info(String.format("execute %s on %s", "beforeDeleteEntityInterceptor", entity.toString()));
                interceptor.beforeDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass);
            }
        }
        return entity;
    }

    public Object executeafterDeleteInterceptor(Object entity, HttpServletRequest request, Map<String, String> additionalData, boolean mainEntity, Class projectionClass) throws ClassNotFoundException, AbortSaveInterceptorException, EntityReflectionException, SkipDeleteInterceptorException {
        log.info(String.format("find %s interceptors on %s...", "afterDeleteEntityInterceptor", entity.toString()));
        List<NextSdrControllerInterceptor> interceptors = getInterceptors(EntityReflectionUtils.getEntityFromProxyObject(entity));
        if (interceptors != null) {
            for (NextSdrControllerInterceptor interceptor : interceptors) {
                log.info(String.format("execute %s on %s", "afterDeleteEntityInterceptor", entity.toString()));
                interceptor.afterDeleteEntityInterceptor(entity, additionalData, request, mainEntity, projectionClass);
            }
        }
        return entity;
    }

    public List<NextSdrControllerInterceptor> getInterceptors(Class entityClass) {
        return interceptorsMap.get(entityClass.getName());
    }

    /**
     * torna "true" se per la classe Entità passata è implementato almeno un
     * beforeSelectQueryInterceptor
     *
     * @param entityClass
     * @return
     */
    public boolean isImplementedBeforeQueryInterceptor(Class entityClass) {
        List<NextSdrControllerInterceptor> interceptors = interceptorsMap.get(entityClass.getName());
        return interceptors != null && interceptors.stream().anyMatch((interceptor) -> (Arrays.stream(interceptor.getClass().getDeclaredMethods()).anyMatch(method -> method.getName().equals(NextSdrControllerInterceptor.BEFORE_SELECT_QUERY_INTERCEPTOR_METHOD_NAME))));
    }

    /** Lancia gli interceptor di tipo after per tutte le entità figlie in base a quanto indicato nella lista passata.
     * L'idea è che quando il RestControllerEngine utilizza le sue procedure per fare i vari aggiornamenti, ogni volta che
     * tenta di lanciare un interceptor di before su una entità secondaria (discendente di quella principale su cui è stata
     * richiesta l'operazione) salva in una lista questo tentativo; alla fine dell'operazione per tutti gli interceptor di before
     * che si è cercato di lanciare, si fa l'equivalente per cercare di lanciare i corrispettivi interceptor di tipo after.
     *
     * @param launchedBeforeInterceptors lista che contiene tutti gli interceptor di tipo before che si è cercato di lanciare
     */
    public void launchAfterInterceptorsOnChildEntities(Object mainEntity, List<ParameterizedInterceptor> launchedBeforeInterceptors) throws AbortSaveInterceptorException, EntityReflectionException, ClassNotFoundException, InvocationTargetException, SkipDeleteInterceptorException, RestControllerEngineException, IllegalAccessException {
        for (ParameterizedInterceptor launchedInterceptor : launchedBeforeInterceptors){
            InterceptorParameters params = launchedInterceptor.getParameters();
            switch (launchedInterceptor.getOperation()){
                case CREATE:
                    // In caso di create, non posso passare all'interceptor l'entità presente nei params in quanto si riferisce all'entità salvata quando è stato lanciato il before
                    // (ossia l'entità pre-salvataggio) che è un oggetto diverso rispetto all'entità persistita attualmente a DB. Per questo utilizzo la lista
                    // di getMethodsPath che indica i vari metodi da richiamare sull'entità principale per arrivare alla nuova entità creata
                    ArrayList<Object> getMethodsPath = launchedInterceptor.getGetMethodsPaths();
                    Object entity = getEntityFromPath(mainEntity, getMethodsPath);
                    if (entity != null)
                        executeafterCreateInterceptor(entity, params.getRequest(), params.getAdditionalData(), params.isMainEntity(), params.getProjection());
                    break;
                case UPDATE:
                    executeafterUpdateInterceptor(params.getEntity(), params.getBeforeUpdateEntity(), params.getRequest(), params.getAdditionalData(), params.isMainEntity(), params.getProjection());
                    break;
                case DELETE:
                    launchNestedDeleteInterceptor(params.getEntity(), params.getRequest(), params.getAdditionalData(), new HashMap(),
                            NextSdrControllerInterceptor.InterceptorType.AFTER, params.isMainEntity(), params.getProjection());
                    break;
            }
        }
    }

    /**
     * Recupera l'entità a partire dalla entità principale indicata e in base alla lista di metodi forniti
     * @param entity entità principale da cui recuperare l'entità di interesse
     * @param getMethodsPath lista con tutti i metodi da richiamare sull'entità principale per arrivare all'entità desiderata
     * @return l'entità a partire dalla entità principale indicata e in base alla lista di metodi forniti
     */
    private Object getEntityFromPath(Object entity, ArrayList<Object> getMethodsPath) {
        Object currEntity = entity;
        try {
            for (Object currObj : getMethodsPath) {
                // Se l'oggetto è un numero, vuol dire che l'entità attualmente recuperata è una collection e questo è l'indice
                // con cui recuperare l'oggetto successivo
                if (currObj.getClass() == int.class || currObj.getClass() == Integer.class) {
                    int pos = (int) currObj;
                    Collection coll = (Collection) currEntity;
                    int i = 0;
                    for (Object currCollObj : coll) {
                        if (i == pos) {
                            currEntity = currCollObj;
                            break;
                        }
                        i++;
                    }
                } else {
                    // Se l'oggetto è un metodo, lo applico sull'entità attuale
                    Method method = (Method) currObj;
                    currEntity = method.invoke(currEntity);
                }
            }
        } catch (Exception e){
            return null;
        }
        return currEntity;
    }

    /**
     * lancia gli interceptor "beforeDelete" sull'entità passata e su tutte le entità figlie
     * TODO: nei casi di molti a molti lancia l'interceptor su entità che non vengono eliminate
     * (nel senso che dovrebbe scattare solo se saranno effettivamente eliminate, bisognerebbe guardare il cascade per decidere se deve scattare oppure no)
     * @param entity
     * @param request
     * @param additionalData
     * @throws ClassNotFoundException
     * @throws AbortSaveInterceptorException
     * @throws EntityReflectionException
     * @throws SkipDeleteInterceptorException
     * @throws RestControllerEngineException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public void launchNestedDeleteInterceptor(Object entity, HttpServletRequest request, Map<String, String> additionalData, Map<String, Boolean> alreadyChecked,
                                               NextSdrControllerInterceptor.InterceptorType interceptorType, boolean mainEntity, Class projectionClass) throws ClassNotFoundException, AbortSaveInterceptorException, EntityReflectionException, SkipDeleteInterceptorException, RestControllerEngineException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (alreadyChecked == null) {
            alreadyChecked = new HashMap();
        }
        alreadyChecked.put(EntityReflectionUtils.getEntityFromProxyObject(entity).getCanonicalName(), true);
        Field[] fields = entity.getClass().getDeclaredFields();
        if (fields != null && fields.length > 0) {
            for (Field field : fields) {
//                Type actualTypeArgument = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
//                Class childEntityClass = Class.forName(actualTypeArgument.getTypeName());
                if (EntityReflectionUtils.isForeignKeyField(field) && Collection.class.isAssignableFrom(field.getType())) {
                    Method getMethod = EntityReflectionUtils.getGetMethod(entity.getClass(), field.getName());
                    Collection childEntityCollection = (Collection) getMethod.invoke(entity);
                    String entityToCheck = ((ParameterizedType) getMethod.getAnnotatedReturnType().getType()).getActualTypeArguments()[0].getTypeName();
                    if (!alreadyChecked.containsKey(entityToCheck) || !alreadyChecked.get(entityToCheck)) {
                        for (Object childEntity : childEntityCollection) {
                            launchNestedDeleteInterceptor(childEntity, request, additionalData, alreadyChecked, interceptorType, false, projectionClass);
                        }
                    }
                }
            }
        }
        if (interceptorType == NextSdrControllerInterceptor.InterceptorType.BEFORE)
            executebeforeDeleteInterceptor(entity, request, additionalData, mainEntity, projectionClass);
        else
            executeafterDeleteInterceptor(entity, request, additionalData, mainEntity, projectionClass);
    }
}
