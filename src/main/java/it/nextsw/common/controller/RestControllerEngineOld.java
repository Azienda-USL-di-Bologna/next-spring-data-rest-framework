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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
public abstract class RestControllerEngineOld {

    private final Logger log = LoggerFactory.getLogger(RestControllerEngineOld.class);

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
            if (StringUtils.hasText(entityPath)) {
                generalRepository = (JpaRepository) customRepositoryPathMap.get(entityPath);
            } else {
                generalRepository = (JpaRepository) getGeneralRepository(request);
            }

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
        if (StringUtils.hasText(entityPath)) {
            generalRepository = (JpaRepository) this.customRepositoryPathMap.get(entityPath);
        } else {
            generalRepository = (JpaRepository) getGeneralRepository(request);
        }
        Class entityClass = EntityReflectionUtils.getEntityClassFromRepository(generalRepository);
        try {
            /**
             * costruzione dell'oggetto entity a partire dalla mappa dei dati
             * "grezzi" espressi in (chiave-valore) passati nella richiesta
             * attraverso fasterxml.jackson
             */
            Object entity = objectMapper.convertValue(data, entityClass);
            boolean inserting = true;

            /**
             * se ho un id seriale e ho passato un id nell'oggetto da inserire
             * lo elimino chiamando il metodo setId passando come valore null
             * (nell'inserimento l'id sarà calcolato e non deve essere
             * considerato se passato).
             */
            if (entityReflectionUtils.hasSerialPrimaryKey(entityClass)) {
                Method primaryKeySetMethod = entityReflectionUtils.getPrimaryKeySetMethod(entity);
                // si ottiene il metodo set della primary key e lo si setta a null
                primaryKeySetMethod.invoke(entity, (Object) null);
                log.warn(String.format("sto invocando %s.%s(%s)", entity.getClass().getSimpleName(), primaryKeySetMethod.getName(), null));
            } else {
                /**
                 * altrimenti estraggo l'id tramite il metodo getId e se c'è
                 * controllo se per caso l'entità con quell'id esista Se esiste,
                 * JPA farà un update invece che un inserimento. Per cui mi
                 * comporto come un update facendo il merge dei campi.
                 */
                Method primaryKeyGetMethod = entityReflectionUtils.getPrimaryKeyGetMethod(entity);
                Object id = primaryKeyGetMethod.invoke(entity);
                if (id != null) {
                    Object foundEntity = em.find(entityClass, id);
                    if (foundEntity != null) {
                        inserting = false;
//                        entity = foundEntity;
                        entity = merge(data, foundEntity, request, additionalData);
                    }
                }
            }

            /**
             * gestione di inserimento / gestione degli oggetti annidati. Qui
             * saranno considerati anche gli eventuali interceptor settati per
             * le entità figlie
             */
            manageNestedEntity(entity, data, request, additionalData);

            /**
             * interceptor su entità padre (gli interceptor su entità figlie
             * sono già state fatte prima). Nel caso di id non seriale, se
             * l'entità esiste già, JPA eseguirà un update (più in alto in
             * questo caso è stato settato il boolean inserting a false) per cui
             * in caso di inserimento effettivo lancio l'intereptor
             * beforeInsert, altrimenti l'interceptor beforeUpdate.
             */
            if (inserting) {
                entity = restControllerInterceptor.executebeforeCreateInterceptor(entity, request, additionalData);
            } else {
                entity = restControllerInterceptor.executebeforeUpdateInterceptor(entity, null, request, additionalData);
            }

            // salvataggio dell'entità
            generalRepository.save(entity);

            // viene ritornata l'entità inserita con tutti i campi, compreso l'id generato
            if (!batch) {
                Class projectionClass = getProjectionClass(null, request);
                entity = factory.createProjection(projectionClass, entity);
            }
            return entity;
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException | SecurityException | ClassNotFoundException | EntityReflectionException | IOException ex) {
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
     * @throws AbortSaveInterceptorException
     * @throws RestControllerEngineException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws NoSuchFieldException
     */
    private void manageNestedEntity(Object fieldValue, Object childMap, HttpServletRequest request, Map<String, String> additionalDataMap) throws ClassNotFoundException, AbortSaveInterceptorException, RestControllerEngineException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException, EntityReflectionException, IOException {
        Map<String, Object> childMapValue = (Map<String, Object>) childMap;
        System.out.println(Arrays.toString(((Map) childMap).values().toArray()));
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
                    Method setFkMethod = getSetMethod(fieldValue.getClass(), fkField.getName());
                    setFkMethod.invoke(fieldValue, fkReference);
                }
            } else {
                /**
                 * in questo caso l'attributo può essere un entità, una lista
                 * oppure un valore base (integer, string, date, ecc,).
                 *
                 */

                String fieldName = key;
                Class entityClass = entityReflectionUtils.getEntityFromProxyObject(fieldValue);

                // campo interessato sull'entità
                Field entityField = entityClass.getDeclaredField(fieldName);

                // metodo set sull'entità che setta il valore sul campo interessato
                Method setMethod = getSetMethod(entityClass, key);

                // metodo get sull'entità che ritorna il valore del campo interessato
                Method getMethod = getGetMethod(entityClass, key);

                // tramite il metodo get ottengo il campo interessato sull'entità
                Object fieldChildValue = getMethod.invoke(fieldValue);

                // estraggo il valore da settare al campo
                Object dataChildValue = childMapValue.get(key);

                // caso un cui il campo interessato è un'entità
                if (fieldChildValue != null && entityReflectionUtils.isEntityClassFromProxyObject(fieldChildValue.getClass())) {

                    // si richiama il metodo ricorsivamente per gestire l'entità
                    manageNestedEntity(fieldChildValue, dataChildValue, request, additionalDataMap);

                    boolean hasSerialPrimaryKey = entityReflectionUtils.hasSerialPrimaryKey(fieldChildValue.getClass());
                    Field primaryKeyField = entityReflectionUtils.getPrimaryKeyField(fieldChildValue.getClass());

                    /**
                     * tiro fuori l'id dell'entità che si vuole
                     * inserire/modificare. l'id si può passare nei seguenti
                     * casi: - si vuole modificare un'entità figlia esistente;
                     * in questo caso l'entità con quell'id DEVE esistere (sennò
                     * verrà inserita) - si vuole inserire una nuova entità con
                     * primary key non seriale; in questo caso l'entità con
                     * quell'id NON DEVE esistere (sennò verrà modificata)
                     */
                    Object id = ((Map<String, Object>) dataChildValue).get(primaryKeyField.getName());
                    if (id != null) {
                        log.info("trovato id: " + id);
                        boolean creatingNewEntity = false;

                        /**
                         * se è stato passato l'id e l'entità non ha una primary
                         * key seriale devo creare una nuova entità e settarla
                         * sull'entità padre con metodo set. Per farlo uso un
                         * trucco: prendo l'entità proxy creata da jackson in
                         * fase si deserializzazione, la serializzo in json e
                         * poi la deserializzo nell'oggetto fieldChildValue.
                         * Fatto questo controllo se l'entità in questione
                         * esiste. Se esiste allora vuol dire che sarà
                         * modificata, altrimenti sarà inserita. infine la setto
                         * sull'entità padre tramite il metodo set.
                         */
                        if (!hasSerialPrimaryKey) {
                            fieldChildValue = objectMapper.readValue(objectMapper.writeValueAsString(fieldChildValue), entityReflectionUtils.getEntityFromProxyObject(fieldChildValue));
                            if (em.find(entityReflectionUtils.getEntityFromProxyObject(fieldChildValue), id) == null) {
                                creatingNewEntity = true;
                            }
                            setMethod.invoke(fieldValue, fieldChildValue);
                        }

//                        fieldChildValue = merge((Map<String, Object>) dataChildValue, fieldChildValue, request, additionalDataMap);
                        // se si creerà una nuova entità lancio l'interceptor beforeCreate, strimenti il beforeUpdate
                        if (creatingNewEntity) {
                            restControllerInterceptor.executebeforeCreateInterceptor(fieldChildValue, request, additionalDataMap);
                        } else {
                            restControllerInterceptor.executebeforeUpdateInterceptor(fieldChildValue, null, request, additionalDataMap);
                        }
                    } else { // in questo caso non è stato passato l'id per cui l'entità è per forza da inserire, per cui lancio l'intereptor beforeCreate
                        fieldChildValue = restControllerInterceptor.executebeforeCreateInterceptor(fieldChildValue, request, additionalDataMap);
                        setMethod.invoke(fieldValue, fieldChildValue); // TODO: probabilmente di questo si può fare a meno, verificare
                    }
                } else if (fieldChildValue != null && Collection.class.isAssignableFrom(fieldChildValue.getClass())) {
                    /**
                     * In qesto caso si è passata una lista di entità. Si
                     * gestisce il caso ciclando su tutti gli oggetti
                     */
                    Collection fieldChildsCollection = (Collection) fieldChildValue;
                    Collection dataChildsCollection = (Collection) dataChildValue;
                    Iterator dataChildsCollectionIterator = dataChildsCollection.iterator();
                    for (Object fieldChildCollectionValue : fieldChildsCollection) {
                        Object dataChildCollectionValue = dataChildsCollectionIterator.next();

                        /**
                         * siccome gli oggetti della lista sono per forza figli
                         * dell'entità padre, togliamo gli eventuali riferimenti
                         * all'entità padre e settiamo forzatamente quello
                         * giusto. Es. se stiamo agendo sull'entità Azienda con
                         * id = 1 e nella lista troviamo: [ { "nome": "gdm",
                         * "cognome": dmg, "fk_idAzienda": { "id": 3 } }, {
                         * "nome": "gdm", "cognome": dmg, "idAzienda": { "id":
                         * 3, "nome": "Ausl Parma" } }, ] devo rimuovere dalla
                         * prima entità l'oggetto "fk_idAzienda" e dalla seconda
                         * l'oggetto "idAzienda", perché si riferiscono a un
                         * azienda diversa da quella di cui la lista è figlia
                         */
                        // filterFieldName mi da il nome del campo interessato (nell'esempio precedente "idAzienda")
                        String filterFieldName = entityReflectionUtils.getFilterFieldName(entityField, entityClass);
                        // vado a rimuovere dal json passato i campi interessati
                        ((Map) ((Map) dataChildCollectionValue)).remove(filterFieldName);
                        ((Map) ((Map) dataChildCollectionValue)).remove("fk_" + filterFieldName);
                        // trovo il metodo set per settare sull'entità l'oggetto padre
                        Method setParentFkMethod = getSetMethod(fieldChildCollectionValue.getClass(), filterFieldName);
                        // setto sull'entità l'oggetto padre corretto (che è appunto l'entità padre, dalla quale ho tirato fuori la collection)
                        setParentFkMethod.invoke(fieldChildCollectionValue, fieldValue);

                        // lancio ricorsivamente il metodo sull'entità per gestire gli eventuali figli
                        manageNestedEntity(fieldChildCollectionValue, dataChildCollectionValue, request, additionalDataMap);

                        // come nel caso delle entità precedenti devo capire se l'entità sarà inserita o modificata per poter lanciare i giusti interceptor
                        boolean hasSerialPrimaryKey = entityReflectionUtils.hasSerialPrimaryKey(fieldChildCollectionValue.getClass());
                        Field primaryKeyField = entityReflectionUtils.getPrimaryKeyField(fieldChildCollectionValue.getClass());
                        Object id = ((Map<String, Object>) dataChildCollectionValue).get(primaryKeyField.getName());
                        if (id == null) {
                            fieldChildCollectionValue = restControllerInterceptor.executebeforeCreateInterceptor(fieldChildCollectionValue, request, additionalDataMap);
                        } else {
                            boolean creatingNewEntity = false;
                            if (!hasSerialPrimaryKey) {
                                fieldChildCollectionValue = objectMapper.readValue(objectMapper.writeValueAsString(fieldChildCollectionValue), entityReflectionUtils.getEntityFromProxyObject(fieldChildCollectionValue));
                                if (em.find(entityReflectionUtils.getEntityFromProxyObject(fieldChildCollectionValue), id) == null) {
                                    creatingNewEntity = true;
                                }
                                ////???
                                //setMethod.invoke(fieldChildValue, fieldChildCollectionValue);
                            }

                            if (creatingNewEntity) {
                                fieldChildCollectionValue = restControllerInterceptor.executebeforeCreateInterceptor(fieldChildCollectionValue, request, additionalDataMap);
                            } else {
                                fieldChildCollectionValue = restControllerInterceptor.executebeforeUpdateInterceptor(fieldChildCollectionValue, null, request, additionalDataMap);
                            }
                        }
                    }

                    /**
                     * Se il campo FK dell'entità ha settato orphanRemoval =
                     * true, allora vuol dire che delle entità potrebbero essere
                     * cancellate. Nel caso queste entità saranno nella mappa
                     * "deletedEntitiesMap". Se ce ne sono eseguo il
                     * beforeDeleteInterceptor.
                     */
                    if (entityReflectionUtils.hasOrphanRemoval(entityField)) {
                        Collection deletedEntities = deletedEntitiesMap.get(getMethod.toGenericString());
                        if (deletedEntities != null && !deletedEntities.isEmpty()) {
                            for (Object deletedEntity : deletedEntities) {
                                try {
                                    restControllerInterceptor.executebeforeDeleteInterceptor(deletedEntity, request, additionalDataMap);
                                } catch (SkipDeleteInterceptorException ex) {
                                    // se l'interceptor lancia l'eccezione "SkipDeleteInterceptorException" reinserisco l'entità nella collection in modo che non venga cancellata
                                    log.info("eliminazione annullata dall'interceptor", ex);
                                    fieldChildsCollection.add(deletedEntity);
                                }
                            }
                        }
                    }
                }
            }
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
        if (StringUtils.hasText(entityPath)) {
            generalRepository = (JpaRepository) customRepositoryPathMap.get(entityPath);
        } else {
            generalRepository = (JpaRepository) getGeneralRepository(request);
        }

        Object entity = get(id, request, entityPath);
        if (entity == null) {
            throw new NotFoundResourceException(String.format("la risorsa con id %s non è stata trovata", id.toString()));
        }

//        Map<String, String> additionalDataMap = parseAdditionalDataIntoMap(additionalData);
        try {
            restControllerInterceptor.executebeforeDeleteInterceptor(entity, request, additionalData);
            generalRepository.delete(entity);
        } catch (ClassNotFoundException | EntityReflectionException ex) {
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

            // si effettua il merge sulla classe padre
            Object res = merge(data, entity, request, additionalData);
            // si effettua il merge sulle entità figlie
            manageNestedEntity(res, data, request, additionalData);

            JpaRepository generalRepository;
            if (StringUtils.hasText(entityPath)) {
                generalRepository = (JpaRepository) customRepositoryPathMap.get(entityPath);
            } else {
                generalRepository = (JpaRepository) getGeneralRepository(request);
            }

            restControllerInterceptor.executebeforeUpdateInterceptor(entity, null, request, additionalData);

            generalRepository.save(res);

            if (!batch) {
                Class projectionClass = getProjectionClass(null, request);
                res = factory.createProjection(projectionClass, res);
            }

            return res;
        } catch (RestControllerEngineException | AbortSaveInterceptorException | ClassNotFoundException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException | EntityReflectionException | IOException ex) {
            throw new RestControllerEngineException("errore nell'update", ex);
        }
    }

    private Object merge(Map<String, Object> data, Object entity, HttpServletRequest request, Map<String, String> additionalDataMap) throws RestControllerEngineException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException, EntityReflectionException, ClassNotFoundException, JsonProcessingException, IOException {
        for (String key : data.keySet()) {
            if (!key.startsWith("fk_")) {
                Object value = data.get(key);

                Method setMethod = getSetMethod(entity.getClass(), key);
                Method getMethod = getGetMethod(entity.getClass(), key);
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
                    } else if (Collection.class.isAssignableFrom(setMethod.getParameterTypes()[0])) {

                        /**
                         * Caso in cui trovo una collection. In questo caso
                         * estraggo la Collection dall'entità e ciclo su tutti
                         * gli elementi
                         */
                        // questo è il tipo della collection(quello scritto tra <>), lo otteniamo dal parametro passato alla set del metodo dell'entità padre
                        Type actualTypeArgument = ((ParameterizedType) setMethod.getGenericParameterTypes()[0]).getActualTypeArguments()[0];
                        Class childEntityClass = Class.forName(actualTypeArgument.getTypeName());

                        // contiene i valori passati
                        Collection childValues = (Collection) value;

                        // contiene gli elementi presenti sull'entità
                        Collection elementsCollection = (Collection) getMethod.invoke(entity);

                        /**
                         * dentro la mappa "deletedEntitiesMap" salvo una
                         * collection che contiene gli elementi che saranno
                         * rimossi al salvataggio (per ogni metodo get di ogni
                         * entità sulla quale ho passato dei valori collection).
                         * Gli elementi che saranno rimossi sono quelli presenti
                         * sull'entità, ma che non sono passati nel json della
                         * richiesta. La chiave della mappa è il nome canonico
                         * del metodo get (es.
                         * it.ausl.bologna.ir.Azienda.setAttivitaList), che
                         * contiene sia il nome dell'entià che il nome della
                         * collection sulla quale si sta agendo.
                         */
                        ArrayList deletedEntities = new ArrayList();

                        // Inizialmente nella collection degli elementi eliminati inserisco tutti gli elementi presenti sull'entità
                        deletedEntities.addAll(elementsCollection);
                        deletedEntitiesMap.put(getMethod.toGenericString(), deletedEntities);

                        // svuoto la collection dell'entità e inserisco gli elementi passati
                        elementsCollection.clear();
                        for (Object childValue : childValues) {
                            Object childEntity = objectMapper.convertValue(childValue, childEntityClass);
                            // per ognuno chiamo ricorsivamente il merge in modo da gestire gli eventuali figli
                            merge((Map<String, Object>) childValue, childEntity, request, additionalDataMap);

                            // aggiungo l'elemento alla collection dell'entità
                            elementsCollection.add(childEntity);
                        }

                        // ora rimuovo gli elementi aggiunti dalla collection "deletedEntities" salvata prima; con questo ottengo gli elementi che saranno eliminati
                        deletedEntities.removeAll(elementsCollection);
                    } else {
                        Class trueEntityClass = entityReflectionUtils.getEntityFromProxyObject(entity);
                        Field field = trueEntityClass.getDeclaredField(key);
                        if (EntityReflectionUtils.isForeignKeyField(field)) {
                            /**
                             * caso in cui l'elemento è un entità singola,
                             * richiamo ricorsivamente il merge sull'oggetto.
                             */
                            Object valueToEntity = getMethod.invoke(entity);
                            merge((Map<String, Object>) value, valueToEntity, request, additionalDataMap);
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
    protected Object getResources(HttpServletRequest request, Object id, String projection, Predicate predicate, Pageable pageable, String additionalData, EntityPathBase path, Class entityClass) throws RestControllerEngineException, AbortLoadInterceptorException {
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
