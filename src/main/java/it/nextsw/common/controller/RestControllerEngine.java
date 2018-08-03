package it.nextsw.common.controller;

import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.RestControllerInterceptorEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.google.common.base.Splitter;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import it.nextsw.common.utils.EntityReflectionUtils;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import it.bologna.ausl.jenesisprojections.tools.ForeignKey;
import it.nextsw.common.interceptors.exceptions.InterceptorException;
import it.nextsw.common.interceptors.exceptions.RollBackInterceptorException;
import it.nextsw.common.projections.ProjectionsInterceptorLauncher;
import it.nextsw.common.repositories.CustomQueryDslRepository;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ResourceAssembler;

/**
 *
 * @author spritz
 */
//@RestController
public abstract class RestControllerEngine {

    @Autowired
    protected EntityReflectionUtils entityReflectionUtils;

    @Value("${custom.mapping.url.root}")
    protected String BASE_URL;

    @Autowired
    private RestControllerInterceptorEngine restControllerInterceptor;

    @Autowired
    protected ProjectionFactory factory;

    @Autowired
    protected PagedResourcesAssembler<Object> assembler;

    @Autowired
    protected ResourceAssembler resourceAssembler;

    @Autowired
    protected ObjectMapper objectMapper;

    @PersistenceContext
    protected EntityManager em;

    @Autowired
    ProjectionsInterceptorLauncher projectionsInterceptorLauncher;

    /**
     * mappa dei repository
     */
    @Autowired
    @Qualifier(value = "customRepositoryMap")
    protected Map<String, CustomQueryDslRepository> customRepositoryMap;

    private Map<String, String> parseAdditionalDataIntoMap(String additionalData) {
        if (additionalData != null && !additionalData.isEmpty()) {
            return Splitter.on(",").withKeyValueSeparator("=").split(additionalData);
        } else {
            return new HashMap<>();
        }
    }

    /**
     * metodo che restituisce, se esiste, l'entity richiesta prendendola dal
     * repository
     *
     * @param id
     * @param request
     * @return
     * @throws RestControllerEngineException
     */
    protected Object get(Object id, HttpServletRequest request) throws RestControllerEngineException {
        Object res = null;
        try {
            JpaRepository generalRepository = (JpaRepository) getGeneralRepository(request);
            Optional<Object> entity = generalRepository.findById(id);
            if (entity.isPresent()) {
                res = entity.get();
            }
        } catch (IllegalArgumentException ex) {
            throw new RestControllerEngineException(ex);
        }
        return res;
    }

    /**
     * Inserimento di una nuova entity
     *
     * @param data - dati grezzi passati nella richiesta
     * @param entityClass - classe dell'entità
     * @param request
     * @param additionalData
     * @return
     * @throws RestControllerEngineException
     * @throws RollBackInterceptorException
     */
    protected Object insert(Map<String, Object> data, Class entityClass, HttpServletRequest request, String additionalData) throws RestControllerEngineException, RollBackInterceptorException {
        Map<String, String> additionalDataMap = parseAdditionalDataIntoMap(additionalData);
        // istanziazione del repository corretto
        JpaRepository generalRepository = (JpaRepository) getGeneralRepository(request);
        try {
            /**
             * costruzione dell'oggetto entity a partire dalla mappa dei dati
             * "grezzi" espressi in (chiave-valore) passati nella richiesta
             * attraverso fasterxml.jackson
             */
            Object entity = objectMapper.convertValue(data, entityClass);

            /**
             * eliminazione dell'eventuale id passato (nell'inserimento l'id
             * sarà calcolato e non deve essere considerato se passato),
             * chiamando il metodo setId passando come valore null
             */
            Method primaryKeySetMethod = entityReflectionUtils.getPrimaryKeySetMethod(entity);
            // si ottiene il metodo set della primary key e lo si setta a null
            primaryKeySetMethod.invoke(entity, (Object) null);
            System.out.println(String.format("sto invocando %s.%s(%s)", entity.getClass().getSimpleName(), primaryKeySetMethod.getName(), null));
            /**
             * gestione di inserimento / gestione degli oggetti annidati. Qui
             * saranno considerati anche gli eventuali interceptor settati per
             * le entità figlie
             */
            manageNestedEntity(true, entity, data, request, additionalDataMap);

            /**
             * interceptor su entità padre (gli interceptor su entità figlie
             * sono già state fatte prima)
             */
            entity = restControllerInterceptor.executebeforeCreateInterceptor(entity, request, additionalDataMap);

            // inserimento dell'entità
            generalRepository.save(entity);
            // viene ritornata l'entità inserita con tutti i campi, compreso l'id generato
            Class projectionClass = getProjectionClass(null, request);
            entity = factory.createProjection(projectionClass, entity);
            return entity;
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException | SecurityException | ClassNotFoundException | EntityReflectionException ex) {
            throw new RestControllerEngineException("errore nell'inserimento", ex);
        }
    }

    /**
     * Gestione delle entity innestate
     *
     * @param fieldValue
     * @param childMap
     * @param request
     * @param additionalDataMap
     * @throws ClassNotFoundException
     * @throws RollBackInterceptorException
     * @throws RestControllerEngineException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws NoSuchFieldException
     */
    private void manageNestedEntity(boolean insert, Object fieldValue, Object childMap, HttpServletRequest request, Map<String, String> additionalDataMap) throws ClassNotFoundException, RollBackInterceptorException, RestControllerEngineException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, EntityReflectionException {
        Map<String, Object> childMapValue = (Map<String, Object>) childMap;
        for (String key : childMapValue.keySet()) {
            /**
             * se sono stati passati dei campi ForeignKey (cioè che iniziano con
             * la stringa "fk_") vuol dire che non si deve inserire l'entità di
             * foreign key, ma usarne una già esistente
             */
            if (key.startsWith("fk_")) {
                /**
                 * si cerca il campo sulla classe dell'entità corrispondente
                 * alla fk, cioè quello il cui nome è ottenuto togliendo il
                 * prefisso "fk_"
                 */
                String fieldName = key.substring("fk_".length());
                Field fkField = entityReflectionUtils.getEntityFromProxyObject(fieldValue).getDeclaredField(fieldName);

                /**
                 * il valore del campo è l'oggetto ForeignKey, del quale serve
                 * solo l'id
                 */
                Object foregnKeyObject = childMapValue.get(key);
                ForeignKey fk = objectMapper.convertValue(foregnKeyObject, ForeignKey.class);

                /**
                 * viene creata l'entità tramite entity manager in modo che
                 * hibernate capisca che non la deve inserire
                 */
                Object fkReference = em.getReference(fkField.getType(), fk.getId());

                /**
                 * si ottiene il campo che setta la fk sulla classe dell'entità
                 * e viene invocato per settarla
                 */
                Method setFkMethod = getSetMethod(fieldValue.getClass(), fkField.getName());
                setFkMethod.invoke(fieldValue, fkReference);
            } else {
                /**
                 * caso in cui è necessario inserire l'entity
                 */
                Method getMethod = getGetMethod(fieldValue.getClass(), key);
                Object fieldChildValue = getMethod.invoke(fieldValue);
                Object dataChildValue = childMapValue.get(key);
                if (fieldChildValue != null && entityReflectionUtils.isEntityClassFromProxyObject(fieldChildValue.getClass())) {
                    if (!insert) {
                        Field primaryKeyField = entityReflectionUtils.getPrimaryKeyField(fieldChildValue.getClass());
                        Object id = ((Map<String, Object>) dataChildValue).get(primaryKeyField.getName());
                        if (id != null) {
                            System.out.println("trovato id: " + id);
                            fieldChildValue = merge((Map<String, Object>) dataChildValue, fieldChildValue, request, additionalDataMap);
                            fieldChildValue = restControllerInterceptor.executebeforeUpdateInterceptor(fieldChildValue, request, additionalDataMap);
                        } else {
                            Method setMethod = getSetMethod(fieldValue.getClass(), key);
                            Class<?> type = entityReflectionUtils.getEntityFromProxyObject(fieldChildValue);
                            Object value = objectMapper.convertValue(dataChildValue, type);
                            fieldChildValue = restControllerInterceptor.executebeforeCreateInterceptor(fieldChildValue, request, additionalDataMap);
                            setMethod.invoke(fieldValue, value);
                        }
                    } else {
                        // togliamo l'eventuale id(passato), chiamando il metodo setId con null
                        Method primaryKeySetMethod = entityReflectionUtils.getPrimaryKeySetMethod(fieldChildValue);
                        System.out.println(String.format("sto invocando %s.%s(%s)", fieldChildValue.getClass().getSimpleName(), primaryKeySetMethod.getName(), null));
                        primaryKeySetMethod.invoke(fieldChildValue, (Object) null);
                        // lanciamo l'interceptor
                        fieldChildValue = restControllerInterceptor.executebeforeCreateInterceptor(fieldChildValue, request, additionalDataMap);
                    }
                    // si richiama il metodo se ci sono altre entità innestate
                    manageNestedEntity(insert, fieldChildValue, dataChildValue, request, additionalDataMap);
                }
            }
        }
    }

    /**
     * Cancellazione di un'entity
     *
     * @param entity - entità da eliminare
     * @param request
     * @param additionalData
     * @throws RestControllerEngineException
     * @throws RollBackInterceptorException
     */
    protected void delete(Object entity, HttpServletRequest request, String additionalData) throws RestControllerEngineException, RollBackInterceptorException {
        JpaRepository generalRepository = (JpaRepository) getGeneralRepository(request);

        Map<String, String> additionalDataMap = parseAdditionalDataIntoMap(additionalData);
        try {
            restControllerInterceptor.executebeforeDeleteInterceptor(entity, request, additionalDataMap);
            generalRepository.delete(entity);
        } catch (ClassNotFoundException | EntityReflectionException ex) {
            throw new RestControllerEngineException("errore nel delete", ex);
        }

    }

    /**
     * Aggiornamento di un'entity esistente
     *
     * @param id
     * @param entity
     * @param data
     * @param request
     * @param additionalData
     * @return
     * @throws RestControllerEngineException
     */
    protected Object update(Object id, Object entity, Map<String, Object> data, HttpServletRequest request, String additionalData) throws RestControllerEngineException {
        try {
            Map<String, String> additionalDataMap = parseAdditionalDataIntoMap(additionalData);
            // si effettua il merge sulla classe padre
            Object res = merge(data, entity, request, additionalDataMap);
            // si effettua il merge sulle entità figlie
            manageNestedEntity(false, res, data, request, additionalDataMap);

            JpaRepository generalRepository = (JpaRepository) getGeneralRepository(request);

            restControllerInterceptor.executebeforeUpdateInterceptor(entity, request, additionalDataMap);

            generalRepository.save(res);
            Class projectionClass = getProjectionClass(null, request);
            res = factory.createProjection(projectionClass, res);

            return res;
        } catch (RestControllerEngineException | RollBackInterceptorException | ClassNotFoundException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException | EntityReflectionException ex) {
            throw new RestControllerEngineException("errore nell'update", ex);
        }
    }

    private Object merge(Map<String, Object> data, Object entity, HttpServletRequest request, Map<String, String> additionalDataMap) throws RestControllerEngineException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException, EntityReflectionException {
        for (String key : data.keySet()) {
            if (!key.startsWith("fk_")) {
                Object value = data.get(key);

                Method setMethod = getSetMethod(entity.getClass(), key);
                if (value != null) {
                    if (setMethod.getParameterTypes()[0].isAssignableFrom(LocalDate.class) || setMethod.getParameterTypes()[0].isAssignableFrom(LocalDateTime.class)) {
//                    String pattern = "yyyy-MM-dd['T'HH:mm:ss.Z]";
//                    DateTimeFormatter format = DateTimeFormatter.ofPattern(pattern);
//                    LocalDateTime dateTime = LocalDateTime.parse(value.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        LocalDateTime dateTime;
                        try {
                            // giorno e ora
                            dateTime = LocalDateTime.parse(value.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        } catch (Exception ex) {
                            // solo giorno
                            //dateTime = LocalDate.parse(value.toString(), format).atStartOfDay();
                            dateTime = LocalDate.parse(value.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")).atStartOfDay();
                        }

//                        Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
                        value = dateTime;
                        setMethod.invoke(entity, value);
                    } else if ((Object[].class).isAssignableFrom(setMethod.getParameterTypes()[0])) {
                        /**
                         * caso in cui il campo che si sta aggiornando è un
                         * Array (questo non è un caso standard e va trattato a
                         * parte, come le date).
                         *
                         * Viene creato un array che contiere oggetti del tipo
                         * identificato dal campo nell'entity; una volta creato
                         * l'array viene popolato con i valori passati nella
                         * richiesta.
                         */
                        value = ((List) value).toArray((Object[]) Array.newInstance(setMethod.getParameterTypes()[0].getComponentType(), 0));
                        /**
                         * invocazione del metodo set del campo passando l'array
                         * costruito partendo dai parametri passati
                         */
                        setMethod.invoke(entity, value);
                    } else {
                        Class trueEntityClass = entityReflectionUtils.getEntityFromProxyObject(entity);
                        Field field = trueEntityClass.getDeclaredField(key);
                        if (entityReflectionUtils.isForeignKeyField(field)) { // caso delle fk
                            Class<?> type = field.getType();
                            value = objectMapper.convertValue(value, type);

                            // eliminiamo il campo id (se presente) nell'entità fk; altrimenti anzichè inserirla andrebbe a modificare quella esistente, identificata dall'id passato
                            Method setFkIdMethod = getSetMethod(value.getClass(), entityReflectionUtils.getPrimaryKeyField(value.getClass()).getName());
                            try {
                                setFkIdMethod.invoke(value, (Object) null);
                            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                                throw new RestControllerEngineException(String.format("errore nell'eliminazione del campo id della fk %s", key), ex);
                            }
                        } else {
                            setMethod.invoke(entity, value);
                        }
                    }
                }
//                setMethod.invoke(entity, value);
            }
        }
        return entity;
    }

    protected CustomQueryDslRepository getGeneralRepository(HttpServletRequest request) throws RestControllerEngineException {
        String repositoryKey = request.getServletPath().substring(BASE_URL.length() + 1);
        int slashPos = repositoryKey.indexOf("/");
        if (slashPos != -1) {
            repositoryKey = repositoryKey.substring(0, slashPos);
        }

        CustomQueryDslRepository generalRepository = customRepositoryMap.get(repositoryKey);
        return generalRepository;
    }

    protected Class getProjectionClass(String projection, HttpServletRequest request) throws RestControllerEngineException {
        Class res;
        if (projection == null) {
            try {
                res = entityReflectionUtils.getDefaultProjection(getGeneralRepository(request));
            } catch (EntityReflectionException ex) {
                throw new RestControllerEngineException(ex);
            }
        } else {
            res = entityReflectionUtils.getProjectionClass(projection);
        }
        return res;
    }

    /**
     * Reperimento delle risorse, considerando il caso si richiedano tutti, una
     * sola (passando 'id) e considerando anche le eventuali projection
     *
     * @param request
     * @param id
     * @param projection
     * @param predicate
     * @param pageable
     * @param additionalData
     * @param path
     * @param entityClass
     * @return
     * @throws RestControllerEngineException
     */
    protected Object getResources(HttpServletRequest request, Object id, String projection, Predicate predicate, Pageable pageable, String additionalData, EntityPathBase path, Class entityClass) throws RestControllerEngineException {
        Object resource = null;
        Class projectionClass;
        /**
         * trasforma gli additionalData espressi in stringa in una mappa vera
         * attraverso un metodo di Google
         */
        Map<String, String> additionalDataMap = parseAdditionalDataIntoMap(additionalData);

        // setto gli additionalData e la request sulla classe che gestisce gli interceptor delle projection, questo metodo svuota anche la cache delle entity sulle projections
        projectionsInterceptorLauncher.setRequestParams(additionalDataMap, request);

        // svuoto la cache delle entity sulle projections
//        projectionsInterceptorLauncher.resetEntityMapCache();

        try {
            // si va a prendere la classe della projection, se viene messa nella chiamata
            projectionClass = getProjectionClass(projection, request);
        } catch (IllegalArgumentException ex) {
            throw new RestControllerEngineException("errore nel reperimento della projection class", ex);
        }

        /**
         * qui serve il repository specifico, ma essendo qui in una funzione
         * generica, tutti i nostri repository estendono
         * CustomQueryDslRepository; così facendo si ha un'interfaccia (quella
         * di repository) che estende un'altra interfaccia
         * (CustomQueryDslRepository), avendo così due tipi disponibili. Così
         * facendo ogni nostro repository è anche di tipo
         * CustomQueryDslRepository.
         *
         * Spring ha una mappa dove la chiave ha il nome della classe in
         * lowerCamelcase mentre il valore corrisponde al valore dei repository
         */
        CustomQueryDslRepository generalRepository = getGeneralRepository(request);
        try {
            // inserimento come predicato dell'eventuale interceptor
            predicate = restControllerInterceptor.executeBeforeSelectQueryInterceptor(predicate, entityClass, request, additionalDataMap);
        } catch (ClassNotFoundException | EntityReflectionException ex) {
            throw new RestControllerEngineException(ex);
        }
        // controllo se è stato passato un id specifico da ricercare
        if (id != null) {
            /**
             * PathBuilder è una classe generica per create predicati;
             * getPrimaryKeyField: metodo che fornisce il nome reale della
             * chiave primaria della classe
             */
            BooleanExpression findByIdExpression = new PathBuilder(
                    BooleanExpression.class, path.getRoot().toString()).
                    get(entityReflectionUtils.getPrimaryKeyField((Class) path.getAnnotatedElement()).getName()).eq(id).
                    and(predicate);
//            Object entity = ((JpaRepository) generalRepository).findById(id).get();
//            new PathBuilder(predicate.getType()-, BASE_URL)

            // avendo la query la si esegue sul repository
            Optional<Object> entityOptional = generalRepository.findOne(findByIdExpression);
            // controllo della presenza del risultato
            if (entityOptional.isPresent()) {
                // si ottiene la entity
                Object entity = entityOptional.get();
                try {
                    // applicazione di afterSelectQueryInterceptor
                    entity = restControllerInterceptor.executeAfterSelectQueryInterceptor(entity, null, entityClass, request, additionalDataMap);
                } catch (ClassNotFoundException | InterceptorException | EntityReflectionException ex) {
                    throw new RestControllerEngineException("errore nell'esecuzione dell'interceptor", ex);
                }
                if (entity != null) {
                    // applicazione della projection nel singolo elemento
                    resource = factory.createProjection(projectionClass, entity);
                }
            }
        } else {
            /**
             * caso in cui non è stato passato un id specifico da ricercare,
             * quindi lo devo fare su tutti i record di una classe
             */
            Page entities = generalRepository.findAll(predicate, pageable);
            try {
                // applicare after select multiplo
                ArrayList<Object> arrayList = new ArrayList<>(entities.getContent());
                /**
                 * si spacchetta la Page che contiene le entity e si passa il
                 * tutto all'interceptor; una volta applicato l'interceptor
                 * viene ricreata la Page
                 */
                List<Object> res = (List<Object>) restControllerInterceptor.executeAfterSelectQueryInterceptor(null, arrayList, entityClass, request, additionalDataMap);
                entities = new PageImpl<>(res, entities.getPageable(), entities.getTotalElements());

            } catch (ClassNotFoundException | InterceptorException | EntityReflectionException ex) {
                throw new RestControllerEngineException("errore nell'esecuzione dell'interceptor", ex);
            }
            // per ogni elemento della pagina gli si applica la createProjection
            Page<Object> projected = entities.map(l -> factory.createProjection(projectionClass, l));
            // assembla il risultato in HAL
            resource = assembler.toResource(projected);
        }
        return resource;
    }

    /**
     * Reperimento del metodo set di un particolare campo, di una particolare
     * classe
     *
     * @param entityClass
     * @param fieldName
     * @return
     * @throws RestControllerEngineException
     */
    private Method getSetMethod(Class entityClass, String fieldName) throws RestControllerEngineException {
        List<Method> methodsFound = new ArrayList<>();
        for (Method declaredMethod : entityClass.getDeclaredMethods()) {
            if (declaredMethod.getName().equals("set" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName))) {
                methodsFound.add(declaredMethod);
            }
        }
        switch (methodsFound.size()) {
            case 0:
                throw new RestControllerEngineException(String.format("metodo set per il campo %s non trovato", fieldName));
            case 2:
                throw new RestControllerEngineException(String.format("trovati più metodi set per il campo %s", fieldName));
        }
        return methodsFound.get(0);
    }

    /**
     * Reperimento del metodo get di un particolare campo, di una particolare
     * classe
     *
     * @param entityClass
     * @param fieldName
     * @return
     * @throws RestControllerEngineException
     */
    private Method getGetMethod(Class entityClass, String fieldName) throws RestControllerEngineException {
        List<Method> methodsFound = new ArrayList<>();
        for (Method declaredMethod : entityClass.getDeclaredMethods()) {
            if (declaredMethod.getName().equals("get" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName))) {
                methodsFound.add(declaredMethod);
            }
        }
        switch (methodsFound.size()) {
            case 0:
                throw new RestControllerEngineException(String.format("metodo get per il campo %s non trovato", fieldName));
            case 2:
                throw new RestControllerEngineException(String.format("trovati più metodi get per il campo %s", fieldName));
        }
        return methodsFound.get(0);
    }

}
