package it.nextsw.common.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.RestControllerInterceptorEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import it.nextsw.common.utils.EntityReflectionUtils;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import it.bologna.ausl.jenesisprojections.tools.ForeignKey;
import it.nextsw.common.controller.exceptions.NotFoundResourceException;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.interceptors.exceptions.InterceptorException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import it.nextsw.common.interceptors.exceptions.SkipDeleteInterceptorException;
import it.nextsw.common.projections.ProjectionsInterceptorLauncher;

import java.io.IOException;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Questa è la classe che deve essere estesa dai controller che vogliono
 * accedere alle funzionalità degli {@link NextSdrInterceptor}
 *
 * @author spritz
 */
public abstract class RestControllerEngine {

    private final Logger log = LoggerFactory.getLogger(RestControllerEngine.class);


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
     * mappa dei repository in cui la chiave è il path completo (baseUrl + "/" + repositoryPath, es. /resources/baborg-api/azienda)
     */
    @Autowired
    @Qualifier(value = "customRepositoryPathMap")
    protected Map<String, NextSdrQueryDslRepository> customRepositoryPathMap;

    /**
     * mappa delle projections
     */
    @Autowired
    @Qualifier(value = "projectionsMap")
    protected Map<String, Class> projectionsMap;

    /**
     * metodo che restituisce, se esiste, l'entity richiesta prendendola dal
     * repository
     *
     * @param id
     * @param request
     * @param entityPath opzionale(serve per le operazione batch), se passata viene usata per reperire il repository, altrimenti il repository viene reperito analizzando la request
     * @return
     * @throws RestControllerEngineException
     */
    protected Object get(Object id, HttpServletRequest request, String entityPath) throws RestControllerEngineException {
        Object res = null;
        try {
            JpaRepository generalRepository;
            if (StringUtils.hasText(entityPath)) {
                generalRepository = (JpaRepository) customRepositoryPathMap.get(entityPath);
            } else {
                generalRepository = (JpaRepository) getGeneralRepository(request, true);
            }

            Class entityClass = EntityReflectionUtils.getEntityClassFromRepository(generalRepository);
            Class<?> pkType = EntityReflectionUtils.getPrimaryKeyField(entityClass).getType();

            Optional<Object> entity = generalRepository.findById(objectMapper.convertValue(id, pkType));
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
     * @param data           - dati grezzi passati nella richiesta
     * @param request
     * @param additionalData
     * @param entityPath     opzionale(serve per le operazione batch), se passata viene usata per reperire il repository, altrimenti il repository viene reperito analizzando la request
     * @param batch          passare true se è la fuunziona viene richiamata in una operazione batch
     * @return l'entità inserita, con la projection base applicata
     * @throws RestControllerEngineException
     * @throws AbortSaveInterceptorException
     */
    protected Object insert(Map<String, Object> data, HttpServletRequest request, Map<String, String> additionalData, String entityPath, boolean batch) throws RestControllerEngineException, AbortSaveInterceptorException {
//        Map<String, String> additionalDataMap = parseAdditionalDataIntoMap(additionalData);

        // istanziazione del repository corretto
        JpaRepository generalRepository;
        if (StringUtils.hasText(entityPath)) {
            generalRepository = (JpaRepository) this.customRepositoryPathMap.get(entityPath);
        } else {
            generalRepository = (JpaRepository) getGeneralRepository(request, false);
        }
        Class entityClass = EntityReflectionUtils.getEntityClassFromRepository(generalRepository);
        try {
            /*
             * istanzio un'entità vuota, che sarà poi "riempita" dal metodo merge più avanti
             */
            Object entity = entityClass.newInstance();
            boolean inserting = true;

            // mi servirà solo nel caso ho passato nei dati dell'entità da inserire un id ed esiste un entità con pk non seriale con quell'id (in questo caso sarà fatto un upadte)
            Object beforeUpdateEntity = null;

            /*
             * se ho un id seriale e ho passato un id nell'oggetto da inserire lo elimino (nell'inserimento l'id sarà calcolato e non deve essere
             * considerato se passato).
             */
            if (EntityReflectionUtils.hasSerialPrimaryKey(entityClass)) {
                String pkFieldName = EntityReflectionUtils.getPrimaryKeyField(entityClass).getName();
                log.warn(String.format("trovato campo %s con valore %s, lo elimino dai dati...", pkFieldName, data.get(pkFieldName)));
                data.remove(pkFieldName);
            } else {
                /*
                 * altrimenti estraggo l'id tramite il metodo getId e se c'è controllo se per caso l'entità con quell'id esista;
                 * se esiste, JPA farà un update invece che un inserimento.
                 * Per cui mi salvo l'entità prima delle modifiche (che saranno effettuate successivamente con il metodo "merge" e setto inserting = false
                 */
                Method primaryKeyGetMethod = EntityReflectionUtils.getPrimaryKeyGetMethod(entity);
                Object id = primaryKeyGetMethod.invoke(entity);
                if (id != null) {
                    Object foundEntity = em.find(entityClass, id);
                    if (foundEntity != null) {
                        inserting = false;
                        entity = foundEntity;
//                        beforeUpdateEntity = objectMapper.convertValue(entity, entityClass);
                        beforeUpdateEntity = cloneEntity(entity);
                    }
                }
            }

            /*
             * Il metodo merge setta i valori passati sull'entità ricorsivamente, inolte lancia i giusti interceptor sui figli.
             */
            entity = merge(data, entity, request, additionalData);

            /*
             * interceptor su entità padre (gli interceptor su entità figlie sono già state fatte prima).
             * Nel caso di id non seriale, se l'entità esiste già, JPA eseguirà un update (in questo caso, più in alto è stato settato il boolean inserting a false)
             * per cui in caso di inserimento effettivo lancio l'intereptor beforeInsert, altrimenti l'interceptor beforeUpdate.
             */
            if (inserting) {
                entity = restControllerInterceptor.executebeforeCreateInterceptor(entity, request, additionalData);
            } else {
                entity = restControllerInterceptor.executebeforeUpdateInterceptor(entity, beforeUpdateEntity, request, additionalData);
            }

            // salvataggio dell'entità
            generalRepository.save(entity);

            /*
             * viene ritornata l'entità inserita con tutti i campi, compreso l'id generato, con la projection base applicata,
             * ma nel caso di batch non applico la projection
             */
            if (!batch) {
                projectionsInterceptorLauncher.setRequestParams(additionalData, request);
                Class projectionClass = getProjectionClass(null, generalRepository);
                entity = factory.createProjection(projectionClass, entity);
            }
            return entity;
        } catch (Exception ex) {
            throw new RestControllerEngineException("errore nell'inserimento", ex);
        }
    }

    /**
     * Cancellazione di un'entity
     *
     * @param id             - id dell'entità da eliminare
     * @param request
     * @param additionalData
     * @param entityPath     opzionale(serve per le operazione batch), se passata viene usata per reperire il repository, altrimenti il repository viene reperito analizzando la request
     * @param batch          passare true se è la fuunziona viene richiamata in una operazione batch
     * @throws RestControllerEngineException
     * @throws AbortSaveInterceptorException
     * @throws NotFoundResourceException
     */
    protected void delete(Object id, HttpServletRequest request, Map<String, String> additionalData, String entityPath, boolean batch) throws RestControllerEngineException, AbortSaveInterceptorException, NotFoundResourceException {
        JpaRepository generalRepository;
        if (StringUtils.hasText(entityPath)) {
            generalRepository = (JpaRepository) customRepositoryPathMap.get(entityPath);
        } else {
            generalRepository = (JpaRepository) getGeneralRepository(request, true);
        }

        Object entity = get(id, request, entityPath);
        if (entity == null) {
            throw new NotFoundResourceException(String.format("la risorsa con id %s non è stata trovata", id.toString()));
        }

//        Map<String, String> additionalDataMap = parseAdditionalDataIntoMap(additionalData);
        try {
            launchNestedBefereDeleteInterceptor(entity, request, additionalData);
            generalRepository.delete(entity);
        } catch (ClassNotFoundException | EntityReflectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RestControllerEngineException("errore nel delete", ex);
        } catch (SkipDeleteInterceptorException ex) {
            log.info("eliminazione annullata dall'interceptor", ex);
        }
    }

    /**
     * Aggiornamento di un'entity esistente
     *
     * @param id
     * @param data
     * @param request
     * @param additionalData
     * @param entityPath     opzionale(serve per le operazione batch), se passata viene usata per reperire il repository, altrimenti il repository viene reperito analizzando la request
     * @param batch          passare true se è la fuunziona viene richiamata in una operazione batch
     * @return
     * @throws RestControllerEngineException
     * @throws it.nextsw.common.controller.exceptions.NotFoundResourceException
     */
    protected Object update(Object id, Map<String, Object> data, HttpServletRequest request, Map<String, String> additionalData, String entityPath, boolean batch) throws RestControllerEngineException, NotFoundResourceException {
        try {
//            Map<String, String> additionalDataMap = parseAdditionalDataIntoMap(additionalData);

            Object entity = get(id, request, entityPath);
            if (entity == null) {
                throw new NotFoundResourceException(String.format("la risorsa con id %s non è stata trovata", id.toString()));
            }

            JpaRepository generalRepository;
            if (StringUtils.hasText(entityPath)) {
                generalRepository = (JpaRepository) customRepositoryPathMap.get(entityPath);
            } else {
                generalRepository = (JpaRepository) getGeneralRepository(request, true);
            }

            boolean willBeEntityModified = willBeEntityModified(entity, data);

            Object res = entity;
            if (willBeEntityModified) {
                Object beforeUpdateEntity = cloneEntity(entity);

                // si effettua il merge sulla classe padre, che andrà in ricorsione anche sulle entità figlie
                res = merge(data, entity, request, additionalData);

                restControllerInterceptor.executebeforeUpdateInterceptor(entity, beforeUpdateEntity, request, additionalData);

                generalRepository.save(res);
            }
            /*
             * viene ritornata l'entità inserita con tutti i campi, compreso l'id generato, con la projection base applicata,
             * ma nel caso di batch non applico la projection
             */
            if (!batch) {
                projectionsInterceptorLauncher.setRequestParams(additionalData, request);
                Class projectionClass = getProjectionClass(null, generalRepository);
                res = factory.createProjection(projectionClass, res);
            }

            return res;
        } catch (Exception ex) {
            throw new RestControllerEngineException("errore nell'update", ex);
        }
    }

    /**
     * Setta i valori presenti nella mappa "data" sull'entità preservando gli altri. Scende ricorsivamenti su tutti i figli e lancia i giusti interceptor
     *
     * @param data              la mappa dei valori da settare
     * @param entity            l'entità sulla quale settare i valori
     * @param request           la request
     * @param additionalDataMap la mappa degli additiol data passati nella rechiesta
     * @return l'oggetto modificato
     * @throws RestControllerEngineException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws NoSuchFieldException
     * @throws EntityReflectionException
     * @throws ClassNotFoundException
     * @throws JsonProcessingException
     * @throws IOException
     * @throws AbortSaveInterceptorException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     */
    protected Object merge(Map<String, Object> data, Object entity, HttpServletRequest request, Map<String, String> additionalDataMap) throws Exception {
        Class entityClass = EntityReflectionUtils.getEntityFromProxyObject(entity);
        String entityPkFieldName = EntityReflectionUtils.getPrimaryKeyField(entityClass).getName();
        boolean hasSerialPrimaryKey = EntityReflectionUtils.hasSerialPrimaryKey(entityClass);
        for (String key : data.keySet()) {

            Object value = data.get(key);
            /*
             * se l'entità ha una pk seriale, devo saltare il settaggio dell'id perché:
             * se sono nel caso di insert deve rimanere null per cui se l'ho passato non lo devo settare altrimenti avrei un errore
             * se sono nel caso di update l'entità in input di questa funzione lo ha già settato e se lo cambiassi avrei un errore
             */
            if (!hasSerialPrimaryKey || !entityPkFieldName.equals(key)) {
                Method setMethod = null;
                Method getMethod = null;
                Field field = null;
                try {          
                    field = EntityReflectionUtils.getDeclaredField(entityClass, key);
                    setMethod = EntityReflectionUtils.getSetMethod(entity.getClass(), key);
                    getMethod = EntityReflectionUtils.getGetMethod(entity.getClass(), key);
                }
                catch (Exception ex) {
                }
                if (field != null && setMethod != null && getMethod != null && EntityReflectionUtils.isColumnOrFkField(field)) {
                    if (value != null) {
                        if (setMethod.getParameterTypes()[0].isAssignableFrom(LocalDate.class) || setMethod.getParameterTypes()[0].isAssignableFrom(LocalDateTime.class)) {
                            manageDateMerge(entity, value, setMethod);
                        } else if ((Object[].class).isAssignableFrom(setMethod.getParameterTypes()[0])) {
                            manageArrayMerge(entity, value, setMethod);
                        } else if (Collection.class.isAssignableFrom(setMethod.getParameterTypes()[0])) {
                            manageCollectionMerge(entity, entityClass, key, (Collection) value, request, additionalDataMap, setMethod, getMethod);

                        } else if (EntityReflectionUtils.isForeignKeyField(field)) {
                            /*
                             * caso in cui l'elemento è un'entità singola,
                             * richiamo ricorsivamente il merge sull'oggetto.
                            */
                            manageChildEntityMerge(entity, entityClass, key, (Map<String, Object>) value, request, additionalDataMap, setMethod, getMethod);
                        } else if (Enum.class.isAssignableFrom(setMethod.getParameterTypes()[0])) {
                            manageEnumMerge(entity, value, setMethod);
                        } else {
                            /*
                             * tutti gli altri casi, cioè l'elemento è un tipo
                             * base (String o Integer, o forse qualche altro
                             * caso che ora non mi viene in mente)
                             */
                            manageOtherCasesMerge(entity, entityClass, key, value, request, additionalDataMap, setMethod, getMethod);
                        }
                    } else {
                        // in questo caso ho passato il valore null per settare il campo a null
                        manageNullValueMerge(entity, entityClass, key, request, additionalDataMap, setMethod, getMethod);
                    }
                }
            }
        }
        return entity;
    }


    /**
     * Gestione delle entità figlie durante il merge
     *
     * @param entity
     * @param entityClass
     * @param key
     * @param value
     * @param request
     * @param additionalDataMap
     * @param setMethod
     * @param getMethod
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws IOException
     * @throws EntityReflectionException
     * @throws RestControllerEngineException
     * @throws ClassNotFoundException
     * @throws AbortSaveInterceptorException
     * @throws NoSuchMethodException
     */
    protected void manageChildEntityMerge(Object entity, Class entityClass, String key, Map<String, Object> value, HttpServletRequest request, Map<String, String> additionalDataMap, Method setMethod, Method getMethod) throws Exception {
        Field field = EntityReflectionUtils.getDeclaredField(entityClass, key);
        Class childEntityClass = field.getType();
        boolean inserting = false;
        Object childEntity = getMethod.invoke(entity);
        Object beforeUpdateEntity = null;
        childEntity = extractCorrectChildEntity(value, childEntity, childEntityClass);
        if (childEntity == null) {
            inserting = true;
            childEntity = field.getType().newInstance();
        }

        boolean willBeEntityModified = willBeEntityModified(childEntity, value);
        if (willBeEntityModified)
            beforeUpdateEntity = cloneEntity(childEntity);
        childEntity = merge(value, childEntity, request, additionalDataMap);
        if (inserting) {
            childEntity = restControllerInterceptor.executebeforeCreateInterceptor(childEntity, request, additionalDataMap);
        } else if (willBeEntityModified)
            childEntity = restControllerInterceptor.executebeforeUpdateInterceptor(childEntity, beforeUpdateEntity, request, additionalDataMap);
        setMethod.invoke(entity, childEntity);
    }

    /**
     * Il metodo si occupa di ritornare l'esatta childEntity confrontando la childEntity settata sull'entità e
     * quella proveniente dal JSON.Per esatta si intende la childEntity con l'id presente nel JSON.
     *
     * @param value
     * @param childEntity
     * @param childEntityClass
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    protected Object extractCorrectChildEntity(Map<String, Object> value, Object childEntity, Class childEntityClass) throws Exception {
        Object valuePKey = value.get(EntityReflectionUtils.getPrimaryKeyField(childEntityClass).getName());
        if (childEntity != null) {
            Object childEntityPKey = EntityReflectionUtils.getPrimaryKeyGetMethod(childEntityClass).invoke(childEntity);
            // se le due primary key hanno tipi diversi provo a convertirli sullo stesso tipo
            if (!childEntityPKey.getClass().equals(valuePKey.getClass())) {
                if (Number.class.isAssignableFrom(childEntityPKey.getClass())) {
                    if (childEntityPKey.getClass().equals(Long.class))
                        valuePKey = ((Number) valuePKey).longValue();
                }
            }
            // se le primary key sono diverse carico l'entità con la primary key indicata nel JSON
            if (!childEntityPKey.equals(valuePKey)) {
                childEntity = em.find(childEntityClass, valuePKey);
            }
        } else {
            childEntity = em.find(childEntityClass, valuePKey);
        }

        return childEntity;
    }

    /**
     * Controlla se l'entità sarà modificata
     * NB: nel caso di classi custom è necessario l'implementazione del metodo equals
     * @param entity
     * @param values
     * @return
     * @throws Exception 
     */
    
    protected boolean willBeEntityModified(Object entity, Map<String, Object> values) throws Exception {
        for (String key : values.keySet()) {
            Object value = values.get(key);
            Method getMethod = null;
            Field field = null;
            try {
                field = EntityReflectionUtils.getDeclaredField(entity.getClass(), key);
                getMethod = EntityReflectionUtils.getGetMethod(entity.getClass(), key);
            }
            catch (Exception ex) {
            }
            if (field != null && getMethod != null && EntityReflectionUtils.isColumnOrFkField(field)) {
                Object valueEntity = getMethod.invoke(entity);
                if (value != valueEntity) {
                    // gestiamo casi null
                    if ((value == null && valueEntity != null) || (value != null && valueEntity == null))
                        return true;

                    Class valueEntityClass = field.getType();
                    if (Enum.class.isAssignableFrom(valueEntityClass) && !Enum.valueOf(valueEntityClass, (String) value).equals(valueEntity))
                        return true;
                    else if (LocalDate.class.isAssignableFrom(valueEntityClass) || LocalDateTime.class.isAssignableFrom(valueEntityClass)) {
                        LocalDateTime dateTime;
                        try {
                            // giorno e ora
                            dateTime = LocalDateTime.parse(value.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        } catch (Exception ex) {
                            // solo giorno
                            //dateTime = LocalDate.parse(value.toString(), format).atStartOfDay();
                            dateTime = LocalDate.parse(value.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")).atStartOfDay();
                        }
                        if (!dateTime.equals(valueEntity))
                            return true;
                    }
                    else if ((Object[].class).isAssignableFrom(valueEntityClass)) {
                        Object[] array = ((List) value).toArray((Object[]) Array.newInstance(valueEntityClass.getComponentType(), 0));
                        if (!Arrays.equals((Object[]) valueEntity, array))
                            return true;
                    }
                    else if (EntityReflectionUtils.isEntityClassFromProxyObject(valueEntityClass)){
                        boolean changedChild = willBeEntityModified(valueEntity, (Map<String, Object>) value);
                        if (changedChild)
                            return true;
                    }
                    else if (Collection.class.isAssignableFrom(valueEntityClass)){
                        Collection collectionValue = (Collection) value;
                        Collection collectionEntity = (Collection)valueEntity;
    //                    boolean hasOrphanRemoval = EntityReflectionUtils.hasOrphanRemoval(field);
                        if (collectionValue.size()!= collectionEntity.size())
                            return true;
                        // questo è il tipo della collection(quello scritto tra <>), lo otteniamo dal parametro passato alla set del metodo dell'entità padre
                        Type actualTypeArgument = ((ParameterizedType) getMethod.getGenericReturnType()).getActualTypeArguments()[0];
                        Class childEntityClass = Class.forName(actualTypeArgument.getTypeName());
                        Field childEntityPrimaryKeyField = EntityReflectionUtils.getPrimaryKeyField(childEntityClass);
                        Method childEntityPrimaryKeyGetMethod = EntityReflectionUtils.getPrimaryKeyGetMethod(childEntityClass);
                        String childValuePrimaryKeyFieldName = childEntityPrimaryKeyField.getName();
                        for (Object valueElement :  collectionValue){
                            Object valueElementPk = ((Map<String, Object>) valueElement).get(childValuePrimaryKeyFieldName);
                            Object childEntityElement = collectionEntity.stream().filter(
                                    e -> {
                                        Object childEntityPk;
                                        try {
                                            childEntityPk = childEntityPrimaryKeyGetMethod.invoke(e);
                                        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                                            return false;
                                        }
                                        if (childEntityPk == null)
                                            return false;
                                        else
                                            return childEntityPk.equals(valueElementPk);
                                    }
                            ).findFirst().orElse(null);
                            if (childEntityElement == null)
                                return true;
                            else {
                                boolean changedChild = willBeEntityModified(childEntityElement, (Map<String, Object>) valueElement);
                                if (changedChild)
                                    return true;
                            }
                        }
                    }
                    // questo if gestisce il caso in cui il campo sia una stringa che rappresenta un json
                    else if(String.class.isAssignableFrom(valueEntityClass) && isJsonParsable((String) valueEntity)) {
                        if (!objectMapper.readTree((String) value).equals(objectMapper.readTree((String) valueEntity))) {
                            return true;
                        }
                    }
                    // questo if gestisce tutti gli altri casi che verosimilmente saranno i tipi base e i tipi primitivi
                    // NB: nel caso di classi custom è necessario l'implementazione del metodo equals
                    else if (!value.equals(valueEntity)) {
                         return true;
                    }
                }
            }
        }
        return false;
    }
    
    protected boolean isJsonParsable(String value) {
        try {
            JsonNode valueJsonNode = objectMapper.readTree((String) value);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Gestisce la merge nel caso in cui il campo sia null.
     * è visto come un metodo che può essere sovrascritto per gestire casistiche paricolari di progetto
     *
     * @param entity
     * @param entityClass
     * @param key
     * @param request
     * @param additionalDataMap
     * @param setMethod
     * @param getMethod
     * @throws Exception
     */
    protected void manageNullValueMerge(Object entity, Class entityClass, String key, HttpServletRequest request, Map<String, String> additionalDataMap, Method setMethod, Method getMethod) throws Exception {
        setMethod.invoke(entity, (Object) null);
    }


    /**
     * Gestisce la merge in tutti gli altri casi, essenzialmente chiama l'invoke sul set per settare il value.
     * è visto come un metodo che può essere sovrascritto per gestire casistiche paricolari di progetto
     *
     * @param entity
     * @param entityClass
     * @param key
     * @param value
     * @param request
     * @param additionalDataMap
     * @param setMethod
     * @param getMethod
     * @throws Exception
     */
    protected void manageOtherCasesMerge(Object entity, Class entityClass, String key, Object value, HttpServletRequest request, Map<String, String> additionalDataMap, Method setMethod, Method getMethod) throws Exception {
        setMethod.invoke(entity, value);
    }


    /**
     * gestione delle enum durante il merge
     *
     * @param entity
     * @param value
     * @param setMethod
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    protected void manageEnumMerge(Object entity, Object value, Method setMethod) throws IllegalAccessException, InvocationTargetException {
        value = Enum.valueOf((Class) setMethod.getParameterTypes()[0], (String) value);
        setMethod.invoke(entity, value);
    }


    /**
     * Gestione degli array durante il merge.
     * caso in cui il campo che si sta aggiornando è un
     * Array (questo non è un caso standard e va trattato a parte, come le date).
     *
     * @param entity
     * @param value
     * @param setMethod
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    protected void manageArrayMerge(Object entity, Object value, Method setMethod) throws IllegalAccessException, InvocationTargetException {
        /*
         * Viene creato un array che contiere oggetti del tipo
         * identificato dal campo nell'entity; una volta creato
         * l'array viene popolato con i valori passati nella
         * richiesta.
         */
        value = ((List) value).toArray((Object[]) Array.newInstance(setMethod.getParameterTypes()[0].getComponentType(), 0));
        /*
         * invocazione del metodo set del campo passando l'array
         * costruito partendo dai parametri passati
         */
        setMethod.invoke(entity, value);
    }

    /**
     * Metodo del merge per la gestione delle date
     *
     * @param entity
     * @param value
     * @param setMethod
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    protected void manageDateMerge(Object entity, Object value, Method setMethod) throws IllegalAccessException, InvocationTargetException {
        LocalDateTime dateTime;
        try {
            // giorno e ora
            dateTime = LocalDateTime.parse(value.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ex) {
            // solo giorno
            //dateTime = LocalDate.parse(value.toString(), format).atStartOfDay();
            dateTime = LocalDate.parse(value.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")).atStartOfDay();
        }
        value = dateTime;
        setMethod.invoke(entity, value);
    }

    /**
     * Caso in cui trovo una collection. In questo caso
     * estraggo la Collection dall'entità e ciclo su tutti gli elementi
     *
     * @param entity
     * @param entityClass
     * @param key
     * @param value
     * @param request
     * @param additionalDataMap
     * @param setMethod
     * @param getMethod
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchFieldException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws RestControllerEngineException
     * @throws IOException
     * @throws EntityReflectionException
     * @throws AbortSaveInterceptorException
     */
    protected void manageCollectionMerge(Object entity, Class entityClass, String key, Collection value, HttpServletRequest request, Map<String, String> additionalDataMap, Method setMethod, Method getMethod) throws Exception {
        // questo è il tipo della collection(quello scritto tra <>), lo otteniamo dal parametro passato alla set del metodo dell'entità padre
        Type actualTypeArgument = ((ParameterizedType) setMethod.getGenericParameterTypes()[0]).getActualTypeArguments()[0];
        Class childEntityClass = Class.forName(actualTypeArgument.getTypeName());

        // contiene i valori passati
        Collection<Map<String, Object>> childValues = value;

        // contiene gli elementi presenti sull'entità
        Collection entityElementsCollection = (Collection) getMethod.invoke(entity);
        if (entityElementsCollection == null) {
            entityElementsCollection = new ArrayList();
            setMethod.invoke(entity, entityElementsCollection);
        }

        // campo interessato sull'entità
        Field entityField = entityClass.getDeclaredField(key);

        ArrayList newElementCollection = new ArrayList();
        Field childPrimaryKeyField = EntityReflectionUtils.getPrimaryKeyField(childEntityClass);
        Method childPrimaryKeyGetMethod = EntityReflectionUtils.getPrimaryKeyGetMethod(childEntityClass);
        for (Map<String, Object> childValue : childValues) {
            Object childEntity;
            boolean inserting = false;
            /* controllo se nell'entità passata è presente l'id (la primary key in generale)
             * se c'è cerco l'oggetto nella lista degli elementi presenti nell'entità:
             * - se lo trovo allora vorrà dire che farò un update
             * - se non lo trovo allora provo a caricarla dal DB, se c'è allora vuol dire che la voglio associare all'entità, altrimenti vorrà dire che farò un insert
             */
            Object childValuePk = childValue.get(childPrimaryKeyField.getName());

            if (childValuePk != null) {
                Optional childEntityOp = entityElementsCollection.stream().filter(e -> {
                    Object pk;
                    try {
                        pk = childPrimaryKeyGetMethod.invoke(e);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        return false;
                    }
                    return pk.equals(childValuePk);
                }).findFirst();
                if (childEntityOp.isPresent()) {
                    inserting = false;
                    childEntity = childEntityOp.get();
                } else {
                    // se la trovo sul DB allora la associo all'entità
                    childEntity = em.find(childEntityClass, childValuePk);
                    if (childEntity == null) { // se non c'è sul DB allora ne creerò una nuova
                        inserting = true;
                        childEntity = childEntityClass.getConstructor().newInstance();
                    }
                }
            } else {
                inserting = true;
                childEntity = childEntityClass.getConstructor().newInstance();
            }
            
            /*
             * Siccome gli oggetti della lista sono per forza figli dell'entità padre, togliamo gli eventuali riferimenti
             * all'entità padre e settiamo forzatamente quello giusto.
             * Es. se stiamo agendo sull'entità Azienda con id = 1 e nella lista troviamo:
             * [ 
             *  {
             *      "nome": "gdm",
             *      "cognome": dmg,
             *      "fk_idAzienda": {
             *          "id": 3
             *      }
             *  },
             *  {
             *      "nome": "gdm",g
             *      "cognome": dmg,
             *      "idAzienda": {
             *          "id": 3,
             *      "nome": "Ausl Parma"
             *      }
             *  } 
             * ]
             * devo rimuovere dalla prima entità l'oggetto "fk_idAzienda" e dalla seconda l'oggetto "idAzienda",
             * perché si riferiscono a un'azienda diversa da quella di cui la lista è figlia
             */

            // filterFieldName mi da il nome del campo interessato (nell'esempio precedente "idAzienda")
            String filterFieldName = EntityReflectionUtils.getFilterFieldName(entityField, entityClass);
            // vado a rimuovere dal json passato i campi interessati
            ((Map) ((Map) childValue)).remove(filterFieldName);
            ((Map) ((Map) childValue)).remove("fk_" + filterFieldName);
            // trovo il metodo set per settare sull'entità l'oggetto padre
            Method setParentFkMethod = EntityReflectionUtils.getSetMethod(childEntity.getClass(), filterFieldName);
            // setto sull'entità l'oggetto padre corretto (che è appunto l'entità padre, dalla quale ho tirato fuori la collection)
            setParentFkMethod.invoke(childEntity, entity);

            Object beforeUpdateEntity = null;
            boolean willBeEntityModified = false;
            if (!inserting) {
                willBeEntityModified = willBeEntityModified(childEntity, childValue);
                if (willBeEntityModified)
                    beforeUpdateEntity = cloneEntity(childEntity);
            }
            // per ognuno chiamo ricorsivamente il merge in modo da gestire gli eventuali figli
            childEntity = merge(childValue, childEntity, request, additionalDataMap);

            if (inserting) {
                childEntity = restControllerInterceptor.executebeforeCreateInterceptor(childEntity, request, additionalDataMap);
            } else if (willBeEntityModified) {
                childEntity = restControllerInterceptor.executebeforeUpdateInterceptor(childEntity, beforeUpdateEntity, request, additionalDataMap);
            }

            // aggiungo l'elemento alla collection dell'entità
            newElementCollection.add(childEntity);
        }

        /*
         * Se il campo FK dell'entità ha settato orphanRemoval = true, allora vuol dire che delle entità potrebbero essere cancellate.
         * Gli elementi che saranno rimossi sono quelli presenti sull'entità, ma che non sono passati nel json della richiesta.
         * Se ci sono elementi che saranno rimossi lancio l'interceptor beforeDelete ricorsivamente su tutti
         */
        if (EntityReflectionUtils.hasOrphanRemoval(entityField)) {
            ArrayList deletedEntities = new ArrayList();

            // Inizialmente nella collection degli elementi eliminati inserisco tutti gli elementi presenti sull'entità
            deletedEntities.addAll(entityElementsCollection);
            // ora rimuovo gli elementi che sto inserendo/modificando; con questo ottengo gli elementi che saranno eliminati
            deletedEntities.removeAll(newElementCollection);
            // deletedEntitiesMap.put(getMethod.toGenericString(), deletedEntities);

            if (!deletedEntities.isEmpty()) {
                for (Object deletedEntity : deletedEntities) {
                    try {
                        launchNestedBefereDeleteInterceptor(deletedEntity, request, additionalDataMap);
                    } catch (SkipDeleteInterceptorException ex) {
                        log.info("delete saltato come richiesto", ex);
                    }
                }
            }
        }

        // infine svuoto la collection dell'entità e inserisco gli elementi passati
        entityElementsCollection.clear();
        entityElementsCollection.addAll(newElementCollection);
    }

    /**
     * Il metodo serve per gestire le fk durante la merge dell'oggetto.
     * Non più usato
     *
     * @param entity
     * @param entityClass
     * @param key
     * @param value
     * @throws NoSuchFieldException
     * @throws EntityReflectionException
     * @throws RestControllerEngineException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    @Deprecated
    protected void manageFkMerge(Object entity, Class entityClass, String key, Object value) throws NoSuchFieldException, EntityReflectionException, RestControllerEngineException, IllegalAccessException, InvocationTargetException {
        /*
         * si cerca il campo sulla classe dell'entità corrispondente
         * alla fk, cioè quello il cui nome è ottenuto togliendo il
         * prefisso "fk_"
         */
        String fieldName = key.substring("fk_".length());
        Field fkField = EntityReflectionUtils.getEntityFromProxyObject(entity).getDeclaredField(fieldName);

        // se il campo fk_ si riferisce a una collection (es. fk_pecUtenteList) lo devo ignorare
        if (!Collection.class.isAssignableFrom(fkField.getType())) {
            /*
             * il valore del campo è l'oggetto ForeignKey, del quale serve
             * solo l'id
             */
            Object foregnKeyObject = value;
            ForeignKey fk = objectMapper.convertValue(foregnKeyObject, ForeignKey.class);

            Object fkReference = null;
            // se si passa "fk_" con id null, vuol dire che voglio settare a null la fk per cui lascio l'oggetto fkReference a null
            if (fk.getId() != null) {
                /*
                 * viene creata l'entità tramite entity manager in modo che
                 * hibernate capisca che non la deve inserire
                 */
                fkReference = em.getReference(fkField.getType(), fk.getId());
            }
            /*
             * si ottiene il campo che setta la fk sulla classe
             * dell'entità e viene invocato per settarla
             */
            Method setFkMethod = EntityReflectionUtils.getSetMethod(entityClass, fkField.getName());
            setFkMethod.invoke(entity, fkReference);
        }
    }

    /**
     * Clona un'entità usando jackson. Prima la trasforma in String e poi crea un oggetto a partire dalla quella.
     * <p>
     * TODO considerare il fatto che potrebbero non esser copiate proprietà identificate con {@link com.fasterxml.jackson.annotation.JsonIgnore} cercare altre soluzioni
     *
     * @param entity l'entità da clonare
     * @return il clone dell'entità
     * @throws IOException
     * @throws EntityReflectionException
     */
    private Object cloneEntity(Object entity) throws IOException, EntityReflectionException {
        return objectMapper.readValue(objectMapper.writeValueAsString(entity), EntityReflectionUtils.getEntityFromProxyObject(entity));
    }

    /**
     * lancia gli interceptor "beforeDelete" sull'entità passata e su tutte le entità figlie
     *
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
    private void launchNestedBefereDeleteInterceptor(Object entity, HttpServletRequest request, Map<String, String> additionalData) throws ClassNotFoundException, AbortSaveInterceptorException, EntityReflectionException, SkipDeleteInterceptorException, RestControllerEngineException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Field[] fields = entity.getClass().getDeclaredFields();
        if (fields != null && fields.length > 0) {
            for (Field field : fields) {
//                Type actualTypeArgument = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
//                Class childEntityClass = Class.forName(actualTypeArgument.getTypeName());
                if (EntityReflectionUtils.isForeignKeyField(field) && Collection.class.isAssignableFrom(field.getType())) {
                    Method getMethod = EntityReflectionUtils.getGetMethod(entity.getClass(), field.getName());
                    Collection childEntityCollection = (Collection) getMethod.invoke(entity);
                    for (Object childEntity : childEntityCollection) {
                        launchNestedBefereDeleteInterceptor(childEntity, request, additionalData);
                    }
                }
            }
        }
        restControllerInterceptor.executebeforeDeleteInterceptor(entity, request, additionalData);
    }

    /**
     * Esegue tutte le operazioni della richista batch
     *
     * @param data    Lista di oggetti "BatchOperation" che indicano le azioni batch da eseguire
     * @param request
     * @return
     * @throws JsonProcessingException
     * @throws RestControllerEngineException
     * @throws AbortSaveInterceptorException
     * @throws NotFoundResourceException
     */
    protected String batch(List<BatchOperation> data, HttpServletRequest request) throws JsonProcessingException, RestControllerEngineException, AbortSaveInterceptorException, NotFoundResourceException {
        for (BatchOperation batchOperation : data) {
            switch (batchOperation.getOperation()) {
                case INSERT:
                    insert(batchOperation.getEntityBody(), request, batchOperation.getAdditionalData(), batchOperation.getEntityPath(), true);
                    break;
                case UPDATE:
                    update(batchOperation.getId(), batchOperation.getEntityBody(), request, batchOperation.getAdditionalData(), batchOperation.getEntityPath(), true);
                    break;
                case DELETE:
                    delete(batchOperation.getId(), request, batchOperation.getAdditionalData(), batchOperation.getEntityPath(), true);
                    break;
            }
        }
        return objectMapper.writeValueAsString(data);
    }

    /**
     * Trova il repositori a partire dalla request usando la servletPath per individuarlo (non funziona nel caso di richiesta batch)
     *
     * @param request
     * @param withId  passare "true" se la richiesta contiene /id
     * @return
     * @throws RestControllerEngineException
     */
    protected NextSdrQueryDslRepository getGeneralRepository(HttpServletRequest request, boolean withId) throws RestControllerEngineException {
        String repositoryKey = request.getServletPath();

        if (withId) {
            int slashPos = repositoryKey.lastIndexOf("/");
            if (slashPos != -1) {
                repositoryKey = repositoryKey.substring(0, slashPos);
            }
        }
        NextSdrQueryDslRepository generalRepository = customRepositoryPathMap.get(repositoryKey);
        return generalRepository;
    }

    /**
     * Trova la classe projection a partire dal nome passato come parametro.
     *
     * @param projection la projection da trovare. Se si passa null viene tornata quella di default (quella base senza espansione di ForeigKey)
     * @param repository il repository dell'entità interessata
     * @return
     * @throws RestControllerEngineException
     */
    protected Class getProjectionClass(String projection, Object repository) throws RestControllerEngineException {
        Class res;
        if (projection == null) {
            try {
                res = EntityReflectionUtils.getDefaultProjection(repository);
            } catch (EntityReflectionException ex) {
                throw new RestControllerEngineException(ex);
            }
        } else {
            res = projectionsMap.get(projection);
        }
        return res;
    }

    /**
     * parsa la stringa additionalData in una mappa Map<String, String>
     *
     * @param additionalData la stringa da parsare
     * @return
     */
    protected Map<String, String> parseAdditionalDataIntoMap(String additionalData) {
        if (additionalData != null && !additionalData.isEmpty()) {
            return Splitter.on(",").withKeyValueSeparator("=").split(additionalData);
        } else {
            return new HashMap<>();
        }
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
    @Transactional(readOnly = true)
    protected Object getResources(HttpServletRequest request, Object id, String projection, Predicate predicate, Pageable pageable, String additionalData, EntityPathBase path, Class entityClass) throws RestControllerEngineException, AbortLoadInterceptorException {
        Object resource = null;
        Class projectionClass;
        /*
         * trasforma gli additionalData espressi in stringa in una mappa vera
         * attraverso un metodo di Google
         */
        Map<String, String> additionalDataMap = parseAdditionalDataIntoMap(additionalData);

        // setto gli additionalData e la request sulla classe che gestisce gli interceptor delle projection, questo metodo svuota anche la cache delle entity sulle projections
        projectionsInterceptorLauncher.setRequestParams(additionalDataMap, request);

        /*
         * Qui serve il repository specifico, ma essendo qui in una funzione generica, tutti i nostri repository estendono
         * NextSdrQueryDslRepository; così facendo si ha un'interfaccia (quella di repository) che estende un'altra interfaccia (NextSdrQueryDslRepository),
         * avendo così due tipi disponibili. Così facendo ogni nostro repository è anche di tipo NextSdrQueryDslRepository.
         *
         * Spring ha una mappa dove la chiave ha il nome della classe in lowerCamelcase mentre il valore corrisponde al valore dei repository
         */
        NextSdrQueryDslRepository generalRepository = getGeneralRepository(request, id != null);
        try {
            // si va a prendere la classe della projection, se viene messa nella chiamata
            projectionClass = getProjectionClass(projection, generalRepository);
        } catch (IllegalArgumentException ex) {
            throw new RestControllerEngineException("errore nel reperimento della projection class", ex);
        }

        try {
            // inserimento come predicato dell'eventuale interceptor
            predicate = restControllerInterceptor.executeBeforeSelectQueryInterceptor(predicate, entityClass, request, additionalDataMap);
        } catch (ClassNotFoundException | EntityReflectionException ex) {
            throw new RestControllerEngineException(ex);
        }
        // controllo se è stato passato un id specifico da ricercare
        if (id != null) {
            /*
             * PathBuilder è una classe generica per create predicati;
             * getPrimaryKeyField: metodo che fornisce il nome reale della
             * chiave primaria della classe
             */
            BooleanExpression findByIdExpression = new PathBuilder(
                    BooleanExpression.class, path.getRoot().toString()).
                    get(EntityReflectionUtils.getPrimaryKeyField((Class) path.getAnnotatedElement()).getName()).eq(id).
                    and(predicate);

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
            /*
             * caso in cui non è stato passato un id specifico da ricercare,
             * quindi lo devo fare su tutti i record di una classe
             */
            Page entities = generalRepository.findAll(predicate, pageable);
            try {
                // applicare after select multiplo
                ArrayList<Object> arrayList = new ArrayList<>(entities.getContent());
                /*
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
//            resource = assembler.toResource(entities);
        }
        return resource;
    }


}
