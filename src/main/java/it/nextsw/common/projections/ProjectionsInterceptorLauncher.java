package it.nextsw.common.projections;

import com.google.common.base.CaseFormat;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import it.nextsw.common.interceptors.RestControllerInterceptorEngine;
import it.nextsw.common.interceptors.exceptions.InterceptorException;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import it.nextsw.common.utils.EntityReflectionUtils;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Component;

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
    @Qualifier(value = "customRepositoryMap")
    protected Map<String, CustomQueryDslRepository> customRepositoryMap;

    @Autowired
    private RestControllerInterceptorEngine restControllerInterceptor;

    @Autowired
    private EntityReflectionUtils entityReflectionUtils;

    @Autowired
    protected ProjectionFactory factory;

    @PersistenceContext
    private EntityManager em;

    private Map<String, String> additionalData;
    private HttpServletRequest request;

    private final Map<String, Object> entityMap = new HashMap<>();

    public void setRequestParams(Map<String, String> additionalData, HttpServletRequest request) {
        this.additionalData = additionalData;
        this.request = request;
    }

    public void resetEntityMapCache() {
        entityMap.clear();
    }

    /**
     * Questo metodo viene lanciato da un annotazione sui campi expand delle
     * projections nei casi di expand di un oggetto singolo (Non List/Set). Alla
     * query dell'expand vengono applicati gli interceptor di Before Select e
     * After Select.
     *
     *
     * @param target: è l'istanza dell'entità su cui sto facendo l'expand
     * @param methodName: è il nome del getter in uso per fare l'expand
     * @param returnType: è la classe dell'entità che sto espandendo (in realtà
     * la classe è una ProxyClass)
     * @return l'oggetto espanso (se non è bloccato dall'interceptor)
     * @throws EntityReflectionException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws ClassNotFoundException
     * @throws InterceptorException
     */
    public Object lanciaInterceptor(Object target, String methodName, Class returnType) throws EntityReflectionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, InterceptorException {
        Class entityFromProxyClass = entityReflectionUtils.getEntityFromProxyClass(returnType); // Recupero la classe che sto espandendo dalla sua ProxyClass

        Method method = target.getClass().getMethod(methodName);    // Recupero il metodo che sto gestendo (quello su cui c'è l'annotazione)
        Object invoke = method.invoke(target);                      // Eseguo il metodo sull'istanza dell'entità di partenza in modo da recuperare l'entità da espandere (In realtà l'esecuzione del metodo non esegue la query ma torna l'istanza dell'entità con popolato solo l'id)
        Method primaryKeyGetMethod = entityReflectionUtils.getPrimaryKeyGetMethod(invoke); // Dall'oggetto precedente prendo il metodo per recuperare la primaryKey dell'entity che sto espandendo
        Object id = primaryKeyGetMethod.invoke(invoke);             // Prendo il valore della primaryKey dell'entità che sto espandendo

        /* Il PathBuilder mi serve per usare tramite reflection le classi QEntity.
         * Il metodo get() è il sostituto dell'operatore '.'
         * Di fatto sto costruendo la query "QEntità.entità.getId.eq(id)"
         */
        BooleanExpression eq = new PathBuilder(
                BooleanExpression.class, CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, entityFromProxyClass.getSimpleName())).
                get(entityReflectionUtils.getPrimaryKeyField((Class) entityFromProxyClass).getName()).eq(id);

        // Mi prendo il repository dell'entità che sto espandendo. Come tipo inserisco CustomQueryDslRepository perché tutti i repositories la estendono.
        CustomQueryDslRepository repo = customRepositoryMap.get(entityFromProxyClass.getSimpleName().toLowerCase());

        // Chiamo l'interceptor del before select il quale mi restitusce il predicato, eventualmente modificato, che userò per fare la query.
        Predicate pred = restControllerInterceptor.executeBeforeSelectQueryInterceptor(eq, entityFromProxyClass, request, additionalData);

        /* L'uso della entityMap serve a cacheare il risultato della query rispetto ad un definito predicato.
         * Quindi a ugual predicato corrisponderà lo stesso risultato senza bisogno di eseguire due volte la stessa query.
         */
        Object entity = entityMap.get(pred.toString());
        if (entity == null) {
            Optional<Object> entityOp = repo.findOne(pred); // Esecuzione della query
            if (entityOp.isPresent()) { // Se la query trova un risultato
                entity = entityOp.get();
                entity = restControllerInterceptor.executeAfterSelectQueryInterceptor(entity, null, returnType, request, additionalData);   // Eseguo l'interceptor after select
                Class<?> projectionClass = entityReflectionUtils.getProjectionClass(entityFromProxyClass.getSimpleName() + "WithPlainFields");  // Recupero la classe della projection con i campi base dell'entità interessata 
                entity = factory.createProjection(projectionClass, entity); // Applico la projection con i campi base al risultato
                entityMap.put(pred.toString(), entity);
            } else {
                // Nel caso la query non torni nulla nella mappa salvo la strina "null". In questo modo evito di rifare la query anche in questi casi
                entityMap.put(pred.toString(), "null");
                entity = null;
            }
        } else if (entity.getClass().isAssignableFrom(String.class) && entity.toString().equals("null")) {
            entity = null;
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
    public Set lanciaInterceptorSet(Object target, String methodName) throws EntityReflectionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, InterceptorException {
        Class targetEntityClass = entityReflectionUtils.getEntityFromProxyObject(target);
        Method method = targetEntityClass.getMethod(methodName);
        // Come returnType voglio il tipo dell'entità all'interno del Set/List. Per trovarlo bisogna castare a ParameterizedType il risultato di getGenericReturnType() sul metodo trattato.
        Class returnType = (Class) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        String returnTypeEntityName = returnType.getSimpleName();

        CustomQueryDslRepository repo = customRepositoryMap.get(returnTypeEntityName.toLowerCase());
        
        String fieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName.substring(3)); // Dal nome del metodo ricavo il nome del campo che sto espdandendo es. UtenteStrutturaSet

        Field field = targetEntityClass.getDeclaredField(fieldName);    // Mi prendo il campo che sto espandendo

        String filterFieldName = entityReflectionUtils.getFilterFieldName(field, returnType);  // Il nome del campo che deve usare nel filtro es. idUtente

        Field primaryKeyField = entityReflectionUtils.getPrimaryKeyField(targetEntityClass); // Questo è il campo della PK, ci serve il suo nome per inserirlo nel calcolo del filtro
        Method primaryKeyGetMethod = entityReflectionUtils.getPrimaryKeyGetMethod(target);  // Prendo il metodo per ottenere la PK
        Object id = primaryKeyGetMethod.invoke(target); // Questo è la PK dell'entity
        
        // Questo predicato corrisponde ad es. a: "QUtenteStruttura.utenteStruttura.idUtente.id.eq(id)"
        Predicate pred = new PathBuilder(
                BooleanExpression.class, CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, returnTypeEntityName)).
                get(filterFieldName).
                get(primaryKeyField.getName()).eq(id);
        
        // Modifico eventualmente il predicato tramite l'interceptor before select 
        pred = restControllerInterceptor.executeBeforeSelectQueryInterceptor(pred, returnType, request, additionalData);
        
        Set entities = (Set) entityMap.get(pred.toString());
        if (entities == null) {
            List entitiesFound = (List) repo.findAll(pred); // Eseguo la query
            // Eseguo l'interceptor after select.
            entitiesFound = (List) restControllerInterceptor.executeAfterSelectQueryInterceptor(null, entitiesFound, returnType, request, additionalData);
//            entities = (Set) targetEntityClass.getMethod(methodName).invoke(target);
            Class<?> projectionClass = entityReflectionUtils.getProjectionClass(returnTypeEntityName + "WithPlainFields"); // Applico la projection base ad ognuno dei risultati della query
            entities = (Set) StreamSupport.stream(entitiesFound.spliterator(), false)
                    .map(l -> factory.createProjection(projectionClass, l)).collect(Collectors.toSet());

            entityMap.put(pred.toString(), entities);
        }

        return entities;
    }
}
