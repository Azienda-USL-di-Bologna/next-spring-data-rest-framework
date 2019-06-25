package it.nextsw.common.projections;

import com.google.common.base.CaseFormat;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import it.nextsw.common.interceptors.RestControllerInterceptorEngine;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.InterceptorException;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import it.nextsw.common.utils.EntityReflectionUtils;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
@Component
public class ProjectionsInterceptorLauncher {

    /**
     * mappa dei repository
     */
    @Autowired
    @Qualifier(value = "customRepositoryEntityMap")
    protected Map<String, NextSdrQueryDslRepository> customRepositoryEntityMap;

    /**
     * mappa delle projections
     */
    @Autowired
    @Qualifier(value = "projectionsMap")
    private Map<String, Class> projectionsMap;

    @Autowired
    private RestControllerInterceptorEngine restControllerInterceptor;

    @Autowired
    protected ProjectionFactory factory;

    private static final ThreadLocal<RequestParams> threadLocalParams = new ThreadLocal<>();

    private class RequestParams {

        public Map<String, Object> entityMap;
        public Map<String, String> additionalData;
        public HttpServletRequest request;

        public RequestParams(Map<String, Object> entityMap, Map<String, String> additionalData, HttpServletRequest request) {
            this.entityMap = entityMap;
            this.additionalData = additionalData;
            this.request = request;
        }
    }

    public void setRequestParams(Map<String, String> additionalData, HttpServletRequest request) {
        RequestParams requestParams = new RequestParams(new HashMap<>(), additionalData, request);
        ProjectionsInterceptorLauncher.threadLocalParams.set(requestParams);
    }

//    public void resetEntityMapCache() {
//        entityMap.clear();
//    }
    /**
     * Questo metodo viene lanciato da un annotazione sui campi expand delle projections nei casi di expand di un oggetto singolo (Non Collection).
     * Alla query dell'expand vengono applicati gli interceptor di Before Select e After Select.
     *
     * @param target: è l'istanza dell'entità su cui sto facendo l'expand
     * @param methodName: è il nome del getter in uso per fare l'expand
     * @return l'oggetto espanso (se non è bloccato dall'interceptor)
     * @throws EntityReflectionException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws ClassNotFoundException
     * @throws InterceptorException
     * @throws it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException
     */
    public Object lanciaInterceptor(Object target, String methodName) throws EntityReflectionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, InterceptorException, AbortLoadInterceptorException {
        return lanciaInterceptor(target, methodName, null);
    }
    public Object lanciaInterceptor(Object target, String methodName, String projectionToUse) throws EntityReflectionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, InterceptorException, AbortLoadInterceptorException {
//        Class entityFromProxyClass = EntityReflectionUtils.getEntityFromProxyClass(returnType); // Recupero la classe che sto espandendo dalla sua ProxyClass

        Method method = target.getClass().getMethod(methodName);    // Recupero il metodo che sto gestendo (quello su cui c'è l'annotazione)
        Class entityFromProxyClass = EntityReflectionUtils.getEntityFromProxyClass(method.getReturnType()); // Recupero la classe che sto espandendo dalla sua ProxyClass
        Object invoke = method.invoke(target);                      // Eseguo il metodo sull'istanza dell'entità di partenza in modo da recuperare l'entità da espandere (In realtà l'esecuzione del metodo non esegue la query ma torna l'istanza dell'entità con popolato solo l'id)

        Object entity = null;
        if (invoke != null) {
            Method primaryKeyGetMethod = EntityReflectionUtils.getPrimaryKeyGetMethod(invoke); // Dall'oggetto precedente prendo il metodo per recuperare la primaryKey dell'entity che sto espandendo
            Object id = primaryKeyGetMethod.invoke(invoke);             // Prendo il valore della primaryKey dell'entità che sto espandendo

            /* Il PathBuilder mi serve per usare tramite reflection le classi QEntity.
             * Il metodo get() è il sostituto dell'operatore '.'
             * Di fatto sto costruendo la query "QEntità.entità.getId.eq(id)"
             */
            Predicate pred = new PathBuilder(
                    BooleanExpression.class, CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, entityFromProxyClass.getSimpleName())).
                    get(EntityReflectionUtils.getPrimaryKeyField((Class) entityFromProxyClass).getName()).eq(id);

            // Mi prendo il repository dell'entità che sto espandendo. Come tipo inserisco NextSdrQueryDslRepository perché tutti i repositories la estendono.
            NextSdrQueryDslRepository repo = customRepositoryEntityMap.get(entityFromProxyClass.getCanonicalName());

            // TODO: forse andrebbe applicata la projection di default inserita nell'apposita nostra annotazione del repository?
            if (!StringUtils.hasText(projectionToUse))
                projectionToUse = entityFromProxyClass.getSimpleName() + "WithPlainFields";
            Class<?> projectionClass = projectionsMap.get(projectionToUse);  // Recupero la classe della projection con i campi base dell'entità interessata

            // controllo se è stato implementato un interceptor before select
            boolean implementedBeforeQueryInterceptor = restControllerInterceptor.isImplementedBeforeQueryInterceptor(entityFromProxyClass);
            //        boolean implementedBeforeQueryInterceptor = true;
            if (implementedBeforeQueryInterceptor) {
                // se è implementato l'interceptor del before select il quale mi restitusce il predicato, eventualmente modificato, che userò per fare la query.
                pred = restControllerInterceptor.executeBeforeSelectQueryInterceptor(pred, entityFromProxyClass, threadLocalParams.get().request, threadLocalParams.get().additionalData, false, projectionClass);
            }

            /* L'uso della entityMap serve a cacheare il risultato della query rispetto ad un definito predicato.
             * Quindi a ugual predicato corrisponderà lo stesso risultato senza bisogno di eseguire due volte la stessa query.
             */
            entity = threadLocalParams.get().entityMap.get(pred.toString());
            if (entity == null) {
                // se è stato implementato un interceptor before select eseguo la query con il predicato calcolato prima
                if (implementedBeforeQueryInterceptor) {
                    Optional<Object> entityOp = repo.findOne(pred); // Esecuzione della query
                    if (entityOp.isPresent()) { // Se la query trova un risultato
                        entity = entityOp.get();
                    } else {
                        entity = null;
                    }
                } else { // altrimenti mi riconduco al caso base tornando direttamente l'oggetto ottenuto chiamando il metodo sull'entità (che teoricamente è più veloce)
                    entity = invoke;
                    // Modificato da Giovanni (probabilmente un refuso di qualche test?)
                    // invoke.getClass().getMethod("getDescrizione").invoke(invoke);
                }
                if (entity != null) {
                    entity = restControllerInterceptor.executeAfterSelectQueryInterceptor(entity, null, entityFromProxyClass, threadLocalParams.get().request, threadLocalParams.get().additionalData, false, projectionClass);   // Eseguo l'interceptor after select

                    entity = factory.createProjection(projectionClass, entity); // Applico la projection con i campi base al risultato
                    threadLocalParams.get().entityMap.put(pred.toString(), entity);
                } else {
                    // Nel caso la query non torni nulla nella mappa salvo la strina "null". In questo modo evito di rifare la query anche in questi casi
                    threadLocalParams.get().entityMap.put(pred.toString(), "null");
                    //                entity = null;
                }
            } else if (entity.getClass().isAssignableFrom(String.class) && entity.toString().equals("null")) {
                entity = null;
            }
        }

        return entity;
    }

    /**
     * Questo metodo viene lanciato da un annotazione sui campi expand delle
     * projections nei casi di expand di liste/set di oggetti. Alla
     * query dell'expand vengono applicati gli interceptor di Before Select e
     * After Select.
     *
     * @param target: è l'istanza dell'entità su cui sto facendo l'expand
     * @param methodName: è il nome del getter in uso per fare l'expand
     * @return
     * @throws EntityReflectionException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @throws InterceptorException
     */
    public Collection lanciaInterceptorCollection(Object target, String methodName)throws EntityReflectionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, InterceptorException, AbortLoadInterceptorException {
        return lanciaInterceptorCollection(target, methodName, null, null);
    }
    
    public Collection lanciaInterceptorCollection(Object target, String methodName, String projectionToUse)throws EntityReflectionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, InterceptorException, AbortLoadInterceptorException {
        return lanciaInterceptorCollection(target, methodName, projectionToUse, null);
    }
    
    /**
     * serve per poter avere la lista ordinata. Per usarla inserire l'annotazione tipo la seguente
     *   @Value("#{@projectionsInterceptorLauncher.lanciaInterceptorCollection(target, 'getTagList', @projectionsInterceptorLauncher.buildSort('asc', 'type', 'description'))}")
     *   @Override
     *   public Object getTagList();
     * @param target entità
     * @param methodName nome del metodo sull'entità da usare che permette di ottenera la lista
     * @param sort ordinamento da usare; usare il metodo buildSort di questa classe per creare l'oggetto Sort
     * @return
     * @throws EntityReflectionException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @throws InterceptorException
     * @throws AbortLoadInterceptorException 
     */
    public Collection lanciaInterceptorCollection(Object target, String methodName, Sort sort)throws EntityReflectionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, InterceptorException, AbortLoadInterceptorException {
        return lanciaInterceptorCollection(target, methodName, null, sort);
    }
    
    public Collection lanciaInterceptorCollection(Object target, String methodName, String projectionToUse, Sort sort) throws EntityReflectionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, InterceptorException, AbortLoadInterceptorException {
        Class targetEntityClass = EntityReflectionUtils.getEntityFromProxyObject(target);
        Method method = targetEntityClass.getMethod(methodName);
        // Come returnType voglio il tipo dell'entità all'interno del Set/List. Per trovarlo bisogna castare a ParameterizedType il risultato di getGenericReturnType() sul metodo trattato.
        Class returnType = (Class) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        String returnTypeEntityName = returnType.getSimpleName();
        boolean isReturnTypeCollection = Collection.class.isAssignableFrom((Class<?>) ((ParameterizedType) method.getGenericReturnType()).getRawType());

        NextSdrQueryDslRepository repo = customRepositoryEntityMap.get(returnType.getCanonicalName());

        String fieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName.substring(3)); // Dal nome del metodo ricavo il nome del campo che sto espdandendo es. UtenteStrutturaSet

        Field field = targetEntityClass.getDeclaredField(fieldName);    // Mi prendo il campo che sto espandendo

        String filterFieldName = EntityReflectionUtils.getFilterFieldName(field, returnType);  // Il nome del campo che deve usare nel filtro es. idUtente

        Field primaryKeyField = EntityReflectionUtils.getPrimaryKeyField(targetEntityClass); // Questo è il campo della PK, ci serve il suo nome per inserirlo nel calcolo del filtro
        Method primaryKeyGetMethod = EntityReflectionUtils.getPrimaryKeyGetMethod(target);  // Prendo il metodo per ottenere la PK
        Object id = primaryKeyGetMethod.invoke(target); // Questo è la PK dell'entity

        PathBuilder pathBuilderPredicate =new PathBuilder(BooleanExpression.class,
                CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, returnTypeEntityName));
        // Questo predicato corrisponde ad es. a: "QUtenteStruttura.utenteStruttura.idUtente.id.eq(id)"
        Predicate pred = isReturnTypeCollection ?
                ((PathBuilder)pathBuilderPredicate
                        .getCollection(filterFieldName, returnType).any())
                        .get(primaryKeyField.getName()).eq(id) :
                pathBuilderPredicate
                        .get(filterFieldName)
                        .get(primaryKeyField.getName()).eq(id);

        // controllo se è stato implementato un interceptor before select
        boolean implementedBeforeQueryInterceptor = restControllerInterceptor.isImplementedBeforeQueryInterceptor(EntityReflectionUtils.getEntityFromProxyClass(returnType));

        //TODO usare collection di default e non la WithPlainFields
        if (!StringUtils.hasText(projectionToUse))
            projectionToUse = returnTypeEntityName + "WithPlainFields";
        Class<?> projectionClass = projectionsMap.get(projectionToUse); // Applico la projection base ad ognuno dei risultati della query

        // se lo è lo eseguo per modificare il predicato
        if (implementedBeforeQueryInterceptor) {
            pred = restControllerInterceptor.executeBeforeSelectQueryInterceptor(pred, returnType, threadLocalParams.get().request, threadLocalParams.get().additionalData, false, projectionClass);
        }

        // vedo se ho già l'entità nella mappa cache
        Collection entities = (Collection) threadLocalParams.get().entityMap.get(pred.toString());
        if (entities == null) {
            Collection entitiesFound;
//            List<NextSdrControllerInterceptor> interceptorsFound = restControllerInterceptor.getInterceptors(entityReflectionUtils.getEntityFromProxyClass(returnType));
            // se  go il predicato before selet implementato allora eseguo la query con il predicato calcolato prima
            if (implementedBeforeQueryInterceptor || sort != null) {
//                System.out.println("query!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                if (sort != null) {
                    entitiesFound = (Collection) repo.findAll(pred, sort); // Eseguo la query
                } else {
                    entitiesFound = (Collection) repo.findAll(pred); // Eseguo la query
                }
            } else { // altrimenti mi riconduco al caso base eseguendo direttamente il metodo sull'entità (che teoricamente è più veloce)
//                System.out.println("NNO query!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                entitiesFound = (Collection) targetEntityClass.getMethod(methodName).invoke(target);
            }

            if (entitiesFound != null) {
                // Eseguo l'interceptor after select.
                entitiesFound = (Collection) restControllerInterceptor.executeAfterSelectQueryInterceptor(null, entitiesFound, returnType, threadLocalParams.get().request, threadLocalParams.get().additionalData, false, projectionClass);

                if (List.class.isAssignableFrom(entitiesFound.getClass())) {
                    entities = (Collection) StreamSupport.stream(entitiesFound.spliterator(), false)
                            .map(l -> factory.createProjection(projectionClass, l)).collect(Collectors.toList());
                } else {
                    entities = (Collection) StreamSupport.stream(entitiesFound.spliterator(), false)
                            .map(l -> factory.createProjection(projectionClass, l)).collect(Collectors.toSet());
                }
                threadLocalParams.get().entityMap.put(pred.toString(), entities);
            }
        }
        return entities;
    }
    
    /**
     * crea un oggetto Sort secondo le proprietà passate
     * @param direction: "asc" o "desc"
     * @param properties nomi dei campi sull'entità per i quali ordinare
     * @return un oggetto Sort secondo le proprietà passate
     */
    public Sort buildSort(String direction, String... properties) {
        return new Sort(Sort.Direction.fromString(direction), properties);
    }
}
