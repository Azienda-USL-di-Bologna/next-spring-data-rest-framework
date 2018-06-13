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
import it.nextsw.common.repositories.CustomQueryDslRepository;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
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
 * @author Utente
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

    protected Object insert(Map<String, Object> data, Class entityClass, HttpServletRequest request, String additionalData) throws RestControllerEngineException, RollBackInterceptorException {
        Map<String, String> additionalDataMap = parseAdditionalDataIntoMap(additionalData);
        JpaRepository generalRepository = (JpaRepository) getGeneralRepository(request);
        try {
            // otteniamo l'oggetto entity a partire dalla mappa dei dati grezzi passati
            Object entity = objectMapper.convertValue(data, entityClass);

            // togliamo l'eventuale id(passato), chiamando il metodo setId con null
            Method primaryKeySetMethod = entityReflectionUtils.getPrimaryKeySetMethod(entity);
            primaryKeySetMethod.invoke(entity, (Object) null);
            System.out.println(String.format("sto invocando %s.%s(%s)", entity.getClass().getSimpleName(), primaryKeySetMethod.getName(), null));
            manageNestedEntity(entity, data, request, additionalDataMap);

            entity = restControllerInterceptor.executebeforeCreateInterceptor(entity, request, additionalDataMap);

            // persistiamo l'entità
            generalRepository.save(entity);
            return entity;
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException | SecurityException | ClassNotFoundException ex) {
            throw new RestControllerEngineException("errore nell'inserimento", ex);
        }
    }

    /**
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
    private void manageNestedEntity(Object fieldValue, Object childMap, HttpServletRequest request, Map<String, String> additionalDataMap) throws ClassNotFoundException, RollBackInterceptorException, RestControllerEngineException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        Map<String, Object> childMapValue = (Map<String, Object>) childMap;
        for (String key : childMapValue.keySet()) {
            // se ci sono stati passati dei campi ForeignKey (cioè che iniziano per "fk_") vuol dire che non dobbiamo inserire l'entità fk, ma usarne una già esistente
            if (key.startsWith("fk_")) {
                // troviamo il campo sulla classe dell'entità corrispondente alla fk (cioè quello il cui nome è ottenuto togliendo il prefisso "fk_")
                Field fkField = fieldValue.getClass().getDeclaredField(key.substring("fk_".length()));

                // il valore del campo è l'oggetto ForeignKey, del quale ci serve solo l'id
                Object foregnKeyObject = childMapValue.get(key);
                ForeignKey fk = objectMapper.convertValue(foregnKeyObject, ForeignKey.class);

                // creiamo l'entità tramite entity manager in modo che hiberante capisca che non la deve inserire
                Object fkReference = em.getReference(fkField.getType(), fk.getId());

                // troviamo il campo che setta la fk sulla classe dell'entità e la settiamo
                Method setFkMethod = getSetMethod(fieldValue.getClass(), fkField.getName());
                setFkMethod.invoke(fieldValue, fkReference);
            } else {
                Method getMethod = getGetMethod(fieldValue.getClass(), key);
                Object fieldChildValue = getMethod.invoke(fieldValue);
                if (fieldChildValue != null && entityReflectionUtils.isEntityClass(fieldChildValue.getClass())) {
                    // togliamo l'eventuale id(passato), chiamando il metodo setId con null
                    Method primaryKeySetMethod = entityReflectionUtils.getPrimaryKeySetMethod(fieldChildValue);
                    primaryKeySetMethod.invoke(fieldChildValue, (Object) null);
                    System.out.println(String.format("sto invocando %s.%s(%s)", fieldChildValue.getClass().getSimpleName(), primaryKeySetMethod.getName(), null));
                    // lanciamo l'iterceptor
                    fieldChildValue = restControllerInterceptor.executebeforeCreateInterceptor(fieldChildValue, request, additionalDataMap);
                    manageNestedEntity(fieldChildValue, childMapValue.get(key), request, additionalDataMap);
                }
            }
        }
    }

//    private Object updateChildEntity(Object entity, Object updatedChildEntity, String fieldName) throws NoSuchFieldException, RestControllerEngineException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
//        Method setMethod = getSetMethod(entity.getClass(), fieldName);
//        setMethod.invoke(entity, updatedChildEntity);
//        return entity;
//    }
    protected void delete(Object entity, HttpServletRequest request, String additionalData) throws RestControllerEngineException, RollBackInterceptorException {
        JpaRepository generalRepository = (JpaRepository) getGeneralRepository(request);

        Map<String, String> additionalDataMap = parseAdditionalDataIntoMap(additionalData);
        try {
            restControllerInterceptor.executebeforeDeleteInterceptor(entity, request, additionalDataMap);
            generalRepository.delete(entity);
        } catch (ClassNotFoundException ex) {
            throw new RestControllerEngineException("errore nel delete", ex);
        }

    }

    protected Object update(Object id, Object entity, Map<String, Object> data, HttpServletRequest request, String additionalData) throws RestControllerEngineException {
        try {
            Map<String, String> additionalDataMap = parseAdditionalDataIntoMap(additionalData);
            Object res = merge(data, entity, request, additionalDataMap);
            manageNestedEntity(res, data, request, additionalDataMap);

            JpaRepository generalRepository = (JpaRepository) getGeneralRepository(request);

            restControllerInterceptor.executebeforeUpdateInterceptor(entity, request, additionalDataMap);

            generalRepository.save(res);
            return res;
        } catch (RestControllerEngineException | RollBackInterceptorException | ClassNotFoundException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException ex) {
            throw new RestControllerEngineException("errore nell'update", ex);
        }
    }

    private Object merge(Map<String, Object> data, Object entity, HttpServletRequest request, Map<String, String> additionalDataMap) throws RestControllerEngineException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
//        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        for (String key : data.keySet()) {
//            boolean noInsertFk = false;
            if (!key.startsWith("fk_")) {
                Object value = data.get(key);
//            if (key.startsWith("fk_")) {
//                System.out.println("key: " + key);
//                key = key.substring("fk_".length());
//                noInsertFk = true;
//            }

                Method setMethod = getSetMethod(entity.getClass(), key);
                if (value != null) {
                    if (setMethod.getParameterTypes()[0].isAssignableFrom(Date.class)) {
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

                        Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
                        value = date;
                    } else {
                        Field field = entity.getClass().getDeclaredField(key);
                        if (entityReflectionUtils.isForeignKeyField(field)) { // caso delle fk
                            Class<?> type = field.getType();
//                        if (noInsertFk) {
//                            ForeignKey fk = objectMapper.convertValue(value, ForeignKey.class);
//                            Object fkReference = em.getReference(type, fk.getId());
//                            value = fkReference;
//                        } else {
                            value = objectMapper.convertValue(value, type);
                            // eliminiamo il campo id (se presente) nell'entità fk; altrimenti anzichè inserirla andrebbe a modificare quella esistente, identificata dall'id passato
                            Method setFkIdMethod = getSetMethod(value.getClass(), entityReflectionUtils.getPrimaryKeyField(value.getClass()).getName());
                            try {
                                setFkIdMethod.invoke(value, (Object) null);
                            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                                throw new RestControllerEngineException(String.format("errore nell'eliminazione del campo id della fk %s", key), ex);
                            }
                        }
                    }
                }
                setMethod.invoke(entity, value);
            }
        }
        return entity;
    }

    protected CustomQueryDslRepository getGeneralRepository(HttpServletRequest request) throws RestControllerEngineException {
//        if (this.repositoryMap == null) {
//            setRepositoryMap();
//        }
        String repositoryKey = request.getServletPath().substring(BASE_URL.length() + 1);
        int slashPos = repositoryKey.indexOf("/");
        if (slashPos != -1) {
            repositoryKey = repositoryKey.substring(0, slashPos);
        }
        CustomQueryDslRepository generalRepository = customRepositoryMap.get(repositoryKey);
//        if (generalRepository == null) {
//            Field[] declaredFields = getClass().getDeclaredFields();
//            Field repositoryField = null;
//            for (Field declaredField : declaredFields) {
//                RepositoryDescriptor annotation = declaredField.getAnnotation(RepositoryDescriptor.class);
//                if (entityReflectionUtils.isRepositoryClass(declaredField.getType())) {
//                    if (request.getServletPath().startsWith(BASE_URL + "/" + annotation.repositoryPath())) {
//                        repositoryField = declaredField;
//                        break;
//                    }
//                }
//            }
//            try {
//                generalRepository = (CustomQueryDslRepository) repositoryField.get(this);
//                GENERAL_REPOSITORY_MAP.put(request.getServletPath(), generalRepository);
//            } catch (IllegalArgumentException | IllegalAccessException ex) {
//                throw new RestControllerEngineException("errore nel reperimento del repository corretto", ex);
//            }
//        }

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

    protected Object getResources(HttpServletRequest request, Object id, String projection, Predicate predicate, Pageable pageable, String additionalData, EntityPathBase path, Class entityClass) throws RestControllerEngineException {
        Object resource = null;
        Class projectionClass;
        Map<String, String> additionalDataMap = parseAdditionalDataIntoMap(additionalData);

        try {
            projectionClass = getProjectionClass(projection, request);
        } catch (IllegalArgumentException ex) {
            throw new RestControllerEngineException("errore nel reperimento della projection class", ex);
        }
        CustomQueryDslRepository generalRepository = getGeneralRepository(request);
        try {
            predicate = restControllerInterceptor.executeBeforeSelectQueryInterceptor(predicate, entityClass, request, additionalDataMap);
        } catch (ClassNotFoundException ex) {
            throw new RestControllerEngineException(ex);
        }
        if (id != null) {
            BooleanExpression findByIdExpression = new PathBuilder(
                    BooleanExpression.class, path.getRoot().toString()).
                    get(entityReflectionUtils.getPrimaryKeyField((Class) path.getAnnotatedElement()).getName()).eq(id).
                    and(predicate);
//            Object entity = ((JpaRepository) generalRepository).findById(id).get();
//            new PathBuilder(predicate.getType()-, BASE_URL)
            Optional<Object> entityOptional = generalRepository.findOne(findByIdExpression);
            if (entityOptional.isPresent()) {
                Object entity = entityOptional.get();
                try {
                    // applicare afterselect interceptor
                    entity = restControllerInterceptor.executeAfterSelectQueryInterceptor(entity, null, entityClass, request, additionalDataMap);
                } catch (ClassNotFoundException | InterceptorException ex) {
                    throw new RestControllerEngineException("errore nell'esecuzione dell'interceptor", ex);
                }
                if (entity != null) {
                    resource = factory.createProjection(projectionClass, entity);
                }
            }
        } else {
            Page entities = generalRepository.findAll(predicate, pageable);
            try {
                // applicare after select multiplo
                ArrayList<Object> arrayList = new ArrayList<>(entities.getContent());
                List<Object> res = (List<Object>) restControllerInterceptor.executeAfterSelectQueryInterceptor(null, arrayList, entityClass, request, additionalDataMap);
//                entities = new PageImpl<>(res, entities.getPageable(), res.size());
                entities = new PageImpl<>(res, entities.getPageable(), entities.getTotalElements());
//                Page<Object> contactDtoPage = entities.map(o -> {  
//                    final ContactDto contactDto = new ContactDto();
//        //get values from contact entity and set them in contactDto
//    //e.g. contactDto.setContactId(contact.getContactId());
//    return contactDto;});
            } catch (ClassNotFoundException | InterceptorException ex) {
                throw new RestControllerEngineException("errore nell'esecuzione dell'interceptor", ex);
            }
            Page<Object> projected = entities.map(l -> factory.createProjection(projectionClass, l));
            resource = assembler.toResource(projected);
        }
        return resource;
    }

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
