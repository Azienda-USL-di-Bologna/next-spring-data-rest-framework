package it.nextsw.common.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.nextsw.common.annotations.NextSdrInterceptor;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.RestControllerInterceptorEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    protected EntityReflectionUtils entityReflectionUtils;

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
    @Qualifier(value = "customRepositoryPathMap")
    protected Map<String, NextSdrQueryDslRepository> customRepositoryPathMap;

    /**
     * mappa delle projections
     */
    @Autowired
    @Qualifier(value = "projectionsMap")
    private Map<String, Class> projectionsMap;

    private final Map<String, Collection> deletedEntitiesMap = new HashMap();

    public abstract String getBaseUrl();

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
            if (StringUtils.hasText(entityPath))
                generalRepository = (JpaRepository) customRepositoryPathMap.get(entityPath);
            else
                generalRepository = (JpaRepository) getGeneralRepository(request);
            
            Class entityClass = EntityReflectionUtils.getEntityClassFromRepository(generalRepository);
            Class<?> pkType = entityReflectionUtils.getPrimaryKeyField(entityClass).getType();

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
     * @param data - dati grezzi passati nella richiesta
     * @param request
     * @param additionalData
     * @param entityPath opzionale(serve per le operazione batch), se passata viene usata per reperire il repository, altrimenti il repository viene reperito analizzando la request
     * @param batch passare true se è la fuunziona viene richiamata in una operazione batch
     * @return l'entità inserita, con la projection base applicata
     * @throws RestControllerEngineException
     * @throws AbortSaveInterceptorException
     */
    
    protected Object insert(Map<String, Object> data, HttpServletRequest request, Map<String, String> additionalData, String entityPath, boolean batch) throws RestControllerEngineException, AbortSaveInterceptorException {
//        Map<String, String> additionalDataMap = parseAdditionalDataIntoMap(additionalData);
        
        // istanziazione del repository corretto
        JpaRepository generalRepository;
        if (StringUtils.hasText(entityPath))
            generalRepository = (JpaRepository) this.customRepositoryPathMap.get(entityPath);
        else
            generalRepository = (JpaRepository) getGeneralRepository(request);
        Class entityClass = EntityReflectionUtils.getEntityClassFromRepository(generalRepository);
        try {
            /**
             * istanzio un'entità vuota, che sarà poi "riempita" dal metodo merge più avanti
             */
            Object entity = entityClass.newInstance();
            boolean inserting = true;
            
            // mi servirà solo nel caso ho passato nei dati dell'entità da inserire un id ed esiste un entità con pk non seriale con quell'id (in questo caso sarà fatto un upadte)
            Object beforeUpdateEntity = null;
            
            /**
             * se ho un id seriale e ho passato un id nell'oggetto da inserire lo elimino (nell'inserimento l'id sarà calcolato e non deve essere
             * considerato se passato).
             */
            if (entityReflectionUtils.hasSerialPrimaryKey(entityClass)) {
                String pkFieldName = entityReflectionUtils.getPrimaryKeyField(entityClass).getName();
                log.warn(String.format("trovato campo %s con valore %s, lo elimino dai dati...", pkFieldName, data.get(pkFieldName)));
                data.remove(pkFieldName);
            } else {
                /**
                 * altrimenti estraggo l'id tramite il metodo getId e se c'è controllo se per caso l'entità con quell'id esista;
                 * se esiste, JPA farà un update invece che un inserimento. 
                 * Per cui mi salvo l'entità prima delle modifiche (che saranno effettuate successivamente con il metodo "merge" e setto inserting = false
                 */
                Method primaryKeyGetMethod = entityReflectionUtils.getPrimaryKeyGetMethod(entity);
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

            /**
             * Il metodo merge setta i valori passati sull'entità ricorsivamente, inolte lancia i giusti interceptor sui figli.
             */
            entity = merge(data, entity, request, additionalData);

            /**
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

            /**
             * viene ritornata l'entità inserita con tutti i campi, compreso l'id generato, con la projection base applicata, 
             * ma nel caso di batch non applico la projection
             */
            if (!batch) {
                Class projectionClass = getProjectionClass(null, request);
                entity = factory.createProjection(projectionClass, entity);
            }
            return entity;
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException | SecurityException | ClassNotFoundException | EntityReflectionException | IOException | InstantiationException ex) {
            throw new RestControllerEngineException("errore nell'inserimento", ex);
        }
    }

    /**
     * Cancellazione di un'entity
     *
     * @param id - id dell'entità da eliminare
     * @param request
     * @param additionalData
     * @param entityPath opzionale(serve per le operazione batch), se passata viene usata per reperire il repository, altrimenti il repository viene reperito analizzando la request
     * @param batch passare true se è la fuunziona viene richiamata in una operazione batch
     * @throws RestControllerEngineException
     * @throws AbortSaveInterceptorException
     * @throws NotFoundResourceException
     */
    protected void delete(Object id, HttpServletRequest request, Map<String, String> additionalData, String entityPath, boolean batch) throws RestControllerEngineException, AbortSaveInterceptorException, NotFoundResourceException {
        JpaRepository generalRepository;
            if (StringUtils.hasText(entityPath))
                generalRepository = (JpaRepository) customRepositoryPathMap.get(entityPath);
            else
                generalRepository = (JpaRepository) getGeneralRepository(request);

        Object entity = get(id, request, entityPath);
        if (entity == null) {
            throw new NotFoundResourceException(String.format("la risorsa con id %s non è stata trovata", id.toString()));
        }

//        Map<String, String> additionalDataMap = parseAdditionalDataIntoMap(additionalData);
        try {
            launchNestedBefereDeleteInterceptor(entity, request, additionalData);
            generalRepository.delete(entity);
        } catch (ClassNotFoundException | EntityReflectionException| IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
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
     * @param entityPath opzionale(serve per le operazione batch), se passata viene usata per reperire il repository, altrimenti il repository viene reperito analizzando la request
     * @param batch passare true se è la fuunziona viene richiamata in una operazione batch
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

//            Object beforeUpdateEntity = objectMapper.convertValue(entity, entityReflectionUtils.getEntityFromProxyObject(entity));
            Object beforeUpdateEntity = cloneEntity(entity);
            
            // si effettua il merge sulla classe padre, che andrà in ricorsione anche sulle entità figlie
            Object res = merge(data, entity, request, additionalData);

            JpaRepository generalRepository;
            if (StringUtils.hasText(entityPath))
                generalRepository = (JpaRepository) customRepositoryPathMap.get(entityPath);
            else
                generalRepository = (JpaRepository) getGeneralRepository(request);

            restControllerInterceptor.executebeforeUpdateInterceptor(entity, beforeUpdateEntity, request, additionalData);

            generalRepository.save(res);

            /**
             * viene ritornata l'entità inserita con tutti i campi, compreso l'id generato, con la projection base applicata, 
             * ma nel caso di batch non applico la projection
             */
            if (!batch) {
                Class projectionClass = getProjectionClass(null, request);
                res = factory.createProjection(projectionClass, res);
            }

            return res;
        } catch (RestControllerEngineException | AbortSaveInterceptorException | ClassNotFoundException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException | InvocationTargetException | EntityReflectionException | IOException | InstantiationException | NoSuchMethodException ex) {
            throw new RestControllerEngineException("errore nell'update", ex);
        }
    }

    private Object merge(Map<String, Object> data, Object entity, HttpServletRequest request, Map<String, String> additionalDataMap) throws RestControllerEngineException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException, EntityReflectionException, ClassNotFoundException, JsonProcessingException, IOException, AbortSaveInterceptorException, InstantiationException, NoSuchMethodException {
        for (String key : data.keySet()) {
            Object value = data.get(key);

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
                Field fkField = entityReflectionUtils.getEntityFromProxyObject(entity).getDeclaredField(fieldName);

                /**
                 * il valore del campo è l'oggetto ForeignKey, del quale serve
                 * solo l'id
                 */
                Object foregnKeyObject = value;
                ForeignKey fk = objectMapper.convertValue(foregnKeyObject, ForeignKey.class);

                // non ha senso passare un campo "fk_" con id null, ma se per caso viene passato, lo ignoriamo per non incorrere in un'eccezione
                if (fk.getId() != null) {
                    /**
                     * viene creata l'entità tramite entity manager in modo che
                     * hibernate capisca che non la deve inserire
                     */
                    Object fkReference = em.getReference(fkField.getType(), fk.getId());

                    /**
                     * si ottiene il campo che setta la fk sulla classe
                     * dell'entità e viene invocato per settarla
                     */
                    Method setFkMethod = getSetMethod(entity.getClass(), fkField.getName());
                    setFkMethod.invoke(entity, fkReference);
                }
            }
            else {
                Method setMethod = getSetMethod(entity.getClass(), key);
                Method getMethod = getGetMethod(entity.getClass(), key);
                if (value != null) {
                    if (setMethod.getParameterTypes()[0].isAssignableFrom(LocalDate.class) || setMethod.getParameterTypes()[0].isAssignableFrom(LocalDateTime.class)) {
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
                    } else if (Collection.class.isAssignableFrom(setMethod.getParameterTypes()[0])) {

                        /**
                         * Caso in cui trovo una collection. In questo caso
                         * estraggo la Collection dall'entità e ciclo su tutti
                         * gli elementi
                         */
                        Class entityClass = entityReflectionUtils.getEntityFromProxyObject(entity);
                        
                        // questo è il tipo della collection(quello scritto tra <>), lo otteniamo dal parametro passato alla set del metodo dell'entità padre
                        Type actualTypeArgument = ((ParameterizedType) setMethod.getGenericParameterTypes()[0]).getActualTypeArguments()[0];
                        Class childEntityClass = Class.forName(actualTypeArgument.getTypeName());

                        // contiene i valori passati
                        Collection<Map<String, Object>> childValues = (Collection) value;

                        // contiene gli elementi presenti sull'entità
                        Collection entityElementsCollection = (Collection) getMethod.invoke(entity);

                        // campo interessato sull'entità
                        Field entityField = entityClass.getDeclaredField(key);
                        

//                        Iterator childValuesIterator = childValues.iterator();
                        ArrayList newElementCollection = new ArrayList();
                        Field childPrimaryKeyField = entityReflectionUtils.getPrimaryKeyField(childEntityClass);
                        Method childPrimaryKeyGetMethod = entityReflectionUtils.getPrimaryKeyGetMethod(childEntityClass);
                        for ( Map<String, Object> childValue : childValues) {
                            Object childEntity;
                            boolean inserting = true;
                            /** controllo se nell'entità passata è presente l'id (la primary key in generale)
                             * se c'è cerco l'oggetto nella lista degli elementi presenti nell'entità:
                             * - se lo trovo allora vorrà dire che farò un update
                             * - se non lo trovo allora vorrà dire che farò un insert
                             */
                            Object childValuePk = childValue.get(childPrimaryKeyField.getName());
                            
                            if (childValuePk != null) {
                                Optional childEntityOp = entityElementsCollection.stream().filter(e -> {
                                    Object pk;
                                    try {
                                        pk = childPrimaryKeyGetMethod.invoke(e);
                                    } catch (Exception ex) {
                                        return false;
                                      } 
                                    return pk.equals(childValuePk);
                                }).findFirst();
                                if (childEntityOp.isPresent()) {
                                    inserting = false;
                                    childEntity = childEntityOp.get();
                                } else {
                                    childEntity = childEntityClass.getConstructor().newInstance();
//                                    childEntity = objectMapper.convertValue(childValue, childEntityClass);
                                }
                            } else {
                                childEntity = childEntityClass.getConstructor().newInstance();
                            }
                            
                            /**
                             * Siccome gli oggetti della lista sono per forza figli dell'entità padre, togliamo gli eventuali riferimenti
                             * all'entità padre e settiamo forzatamente quello giusto. 
                             * Es. se stiamo agendo sull'entità Azienda con id = 1 e nella lista troviamo: 
                             *[ {
                             *    "nome": "gdm",
                             *    "cognome": dmg,
                             *    "fk_idAzienda": {
                             *     "id": 3
                             *    }
                             *  },
                             *  {
                             *   "nome": "gdm",
                             *   "cognome": dmg,
                             *   "idAzienda": {
                             *    "id": 3,
                             *    "nome": "Ausl Parma"
                             *    }
                             *  } ]               
                             * devo rimuovere dalla prima entità l'oggetto "fk_idAzienda" e dalla seconda l'oggetto "idAzienda",
                             * perché si riferiscono a un'azienda diversa da quella di cui la lista è figlia
                             */
                            // filterFieldName mi da il nome del campo interessato (nell'esempio precedente "idAzienda")
                            String filterFieldName = entityReflectionUtils.getFilterFieldName(entityField, entityClass);
                            // vado a rimuovere dal json passato i campi interessati
                            ((Map) ((Map) childValue)).remove(filterFieldName);
                            ((Map) ((Map) childValue)).remove("fk_" + filterFieldName);
                            // trovo il metodo set per settare sull'entità l'oggetto padre
                            Method setParentFkMethod = getSetMethod(childEntity.getClass(), filterFieldName);
                            // setto sull'entità l'oggetto padre corretto (che è appunto l'entità padre, dalla quale ho tirato fuori la collection)
                            setParentFkMethod.invoke(childEntity, entity);
                            
                            Object beforeUpdateEntity = null;
                            if (!inserting) {
//                                beforeUpdateEntity = objectMapper.convertValue(childEntity, childEntityClass);
                                beforeUpdateEntity = cloneEntity(childEntity);
                            }
                            // per ognuno chiamo ricorsivamente il merge in modo da gestire gli eventuali figli
                            childEntity = merge(childValue, childEntity, request, additionalDataMap);
                            
                            if (inserting) {
                                childEntity = restControllerInterceptor.executebeforeCreateInterceptor(childEntity, request, additionalDataMap);
                            } else {
                                childEntity = restControllerInterceptor.executebeforeUpdateInterceptor(childEntity, beforeUpdateEntity, request, additionalDataMap);
                            }

                            // aggiungo l'elemento alla collection dell'entità
                            newElementCollection.add(childEntity);
                        }

                        /**
                         * Se il campo FK dell'entità ha settato orphanRemoval = true, allora vuol dire che delle entità potrebbero essere cancellate.
                         * Gli elementi che saranno rimossi sono quelli presenti sull'entità, ma che non sono passati nel json della richiesta.
                         * Se ci sono elementi che saranno rimossi lancio l'interceptor beforeDelete ricorsivamente su tutti
                        */
                        if (entityReflectionUtils.hasOrphanRemoval(entityField)) {
                            ArrayList deletedEntities = new ArrayList();

                            // Inizialmente nella collection degli elementi eliminati inserisco tutti gli elementi presenti sull'entità
                            deletedEntities.addAll(entityElementsCollection);
                            // ora rimuovo gli elementi che sto inserendo/modificando; con questo ottengo gli elementi che saranno eliminati
                            deletedEntities.removeAll(newElementCollection);
    //                        deletedEntitiesMap.put(getMethod.toGenericString(), deletedEntities);

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
                    } else {
                        Class trueEntityClass = entityReflectionUtils.getEntityFromProxyObject(entity);
                        Field field = trueEntityClass.getDeclaredField(key);
                        /**
                         * caso in cui l'elemento è un entità singola,
                         * richiamo ricorsivamente il merge sull'oggetto.
                         */
                        if (entityReflectionUtils.isForeignKeyField(field)) {
                            boolean inserting = false;
                            Object childEntity = getMethod.invoke(entity);
                            Object beforeUpdateEntity = null;
                            if (childEntity == null) {
                                inserting = true;
                                childEntity = field.getType().newInstance();
                            } else {
//                                beforeUpdateEntity = objectMapper.convertValue(childEntity, entityReflectionUtils.getEntityFromProxyObject(childEntity));
                                beforeUpdateEntity = cloneEntity(childEntity);
                            }

                            childEntity = merge((Map<String, Object>) value, childEntity, request, additionalDataMap);
                            if (inserting) {
                                childEntity = restControllerInterceptor.executebeforeCreateInterceptor(childEntity, request, additionalDataMap);
                            }
                            else {
                                childEntity = restControllerInterceptor.executebeforeUpdateInterceptor(childEntity, beforeUpdateEntity, request, additionalDataMap);
                            }
                            setMethod.invoke(entity, childEntity);
                        } else {
                            /**
                             * tutti gli altri casi, cioè l'elemento è un tipo
                             * base (String o Integer, o forse qualche altro
                             * caso che ora non mi viene in mente)
                             */
                            setMethod.invoke(entity, value);
                        }
                    }
                }
            }         
        }
        return entity;
    }

    /**
     * Clona un'entità usando jackson. Prima la trasforma in String e poi crea un oggetto a partire dalla quella.
     * @param entity l'entità da clonare
     * @return il clone dell'entità
     * @throws IOException
     * @throws EntityReflectionException 
    */
    private Object cloneEntity(Object entity) throws IOException, EntityReflectionException {
        return objectMapper.readValue(objectMapper.writeValueAsString(entity), entityReflectionUtils.getEntityFromProxyObject(entity));
    }
    
    private void launchNestedBefereDeleteInterceptor(Object entity, HttpServletRequest request, Map<String, String> additionalData) throws ClassNotFoundException, AbortSaveInterceptorException, EntityReflectionException, SkipDeleteInterceptorException, RestControllerEngineException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Field[] fields = entity.getClass().getDeclaredFields();
        if (fields != null && fields.length > 0) {
            for (Field field : fields) {
                if (Collection.class.isAssignableFrom(field.getType())) {
                    Method getMethod = getGetMethod(entity.getClass(), field.getName());
                    Collection childEntityCollection = (Collection) getMethod.invoke(entity);
                    for (Object childEntity : childEntityCollection) {
                        launchNestedBefereDeleteInterceptor(childEntity, request, additionalData);
                    }
                }
            }
        }
        restControllerInterceptor.executebeforeDeleteInterceptor(entity, request, additionalData);
    }

    protected String batch(List<BatchOperation> data, HttpServletRequest request, String additionalData) throws JsonProcessingException, RestControllerEngineException, AbortSaveInterceptorException, NotFoundResourceException {
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
    
    protected NextSdrQueryDslRepository getGeneralRepository(HttpServletRequest request) throws RestControllerEngineException {
        String repositoryKey = request.getServletPath().substring(getBaseUrl().length() + 1);
        int slashPos = repositoryKey.indexOf("/");
        if (slashPos != -1) {
            repositoryKey = repositoryKey.substring(0, slashPos);
        }

        NextSdrQueryDslRepository generalRepository = customRepositoryPathMap.get(repositoryKey);
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
            res = projectionsMap.get(projection);
        }
        return res;
    }

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
         * NextSdrQueryDslRepository; così facendo si ha un'interfaccia (quella
         * di repository) che estende un'altra interfaccia
         * (NextSdrQueryDslRepository), avendo così due tipi disponibili. Così
         * facendo ogni nostro repository è anche di tipo
         * NextSdrQueryDslRepository.
         *
         * Spring ha una mappa dove la chiave ha il nome della classe in
         * lowerCamelcase mentre il valore corrisponde al valore dei repository
         */
        NextSdrQueryDslRepository generalRepository = getGeneralRepository(request);
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
//            resource = assembler.toResource(entities);
        }
        return resource;
    }

    /**
     * Reperimento del metodo set di un particolare campo, di una particolare classe
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
     * Reperimento del metodo get di un particolare campo, di una particolare classe
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
