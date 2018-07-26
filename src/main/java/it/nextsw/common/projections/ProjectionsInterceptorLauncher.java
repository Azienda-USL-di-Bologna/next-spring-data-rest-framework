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
    
    public Object lanciaInterceptor(Object target, String methodName, Class returnType) throws EntityReflectionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, InterceptorException {
//        System.out.println("in lanciaInterceptor");
        Class entityFromProxyClass = entityReflectionUtils.getEntityFromProxyClass(returnType);
        
        Method method = target.getClass().getMethod(methodName);
        Object invoke = method.invoke(target);
        Method primaryKeyGetMethod = entityReflectionUtils.getPrimaryKeyGetMethod(invoke);
        Object id = primaryKeyGetMethod.invoke(invoke);
                
        BooleanExpression eq = new PathBuilder(
                    BooleanExpression.class, CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, entityFromProxyClass.getSimpleName())).
                    get(entityReflectionUtils.getPrimaryKeyField((Class) entityFromProxyClass).getName()).eq(id);
   
        CustomQueryDslRepository repo = customRepositoryMap.get(entityFromProxyClass.getSimpleName().toLowerCase());
        
        Predicate pred = restControllerInterceptor.executeBeforeSelectQueryInterceptor(eq, entityFromProxyClass, request, additionalData);
        
        Object entity = entityMap.get(pred.toString());
        if (entity == null) {
            Optional<Object> entityOp = repo.findOne(pred);
            if (entityOp.isPresent()) {
                entity = entityOp.get();
                entity = restControllerInterceptor.executeAfterSelectQueryInterceptor(entity, null, returnType, request, additionalData);
                Class<?> projectionClass = entityReflectionUtils.getProjectionClass(entityFromProxyClass.getSimpleName() + "WithPlainFields");
                entity = factory.createProjection(projectionClass, entity);
                entityMap.put(pred.toString(), entity);
            }
            else {
                entityMap.put(pred.toString(), "null");
                entity = null;
            }
        }
        else if (entity.getClass().isAssignableFrom(String.class) && entity.toString().equals("null")){
            entity = null;
        }
        return entity;
    }
    
    public Set lanciaInterceptorSet(Object target, String methodName) throws EntityReflectionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, InterceptorException {
        Class targetEntityClass = entityReflectionUtils.getEntityFromProxyObject(target);
        Method method =targetEntityClass.getMethod(methodName);
        Class returnType = (Class) ((ParameterizedType)method.getGenericReturnType()).getActualTypeArguments()[0];
        String returnTypeEntityName = returnType.getSimpleName();

        CustomQueryDslRepository repo = customRepositoryMap.get(returnTypeEntityName.toLowerCase());
        
        String fieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName.substring(3));
        
        Field field = targetEntityClass.getDeclaredField(fieldName);
        
        String filterFieldName = entityReflectionUtils.getFilterFieldName(field, returnType);

        Field primaryKeyField = entityReflectionUtils.getPrimaryKeyField(targetEntityClass);
        Method primaryKeyGetMethod = entityReflectionUtils.getPrimaryKeyGetMethod(target);
        Object id = primaryKeyGetMethod.invoke(target);
        
        Predicate pred = new PathBuilder(
            BooleanExpression.class, CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, returnTypeEntityName)).
            get(filterFieldName).
            get(primaryKeyField.getName()).eq(id);
   
        pred = restControllerInterceptor.executeBeforeSelectQueryInterceptor(pred, returnType, request, additionalData);
        
        Set entities = (Set) entityMap.get(pred.toString());
        if (entities == null) {
//            JPAQuery queryDSL = new JPAQuery(em);
//            queryDSL.select(QUtenteStruttura)
//                    .from(new PathBuilder(
//            BooleanExpression.class, CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, returnTypeEntityName)).get(filterFieldName))
//                    .where(pred);
//            entities = new HashSet(queryDSL.fetch());
            
            List entitiesFound = (List) repo.findAll(pred);
            entitiesFound = (List) restControllerInterceptor.executeAfterSelectQueryInterceptor(null, entitiesFound, returnType, request, additionalData);
//            entities = (Set) targetEntityClass.getMethod(methodName).invoke(target);
            Class<?> projectionClass = entityReflectionUtils.getProjectionClass(returnTypeEntityName + "WithPlainFields");
            entities = (Set) StreamSupport.stream(entitiesFound.spliterator(), false)
                    .map(l -> factory.createProjection(projectionClass, l)).collect(Collectors.toSet());
             
//            HashSet newHashSet = Sets.newHashSet(entitiesFound);
//            entities = (Set) entities.stream().map(l -> factory.createProjection(projectionClass, l)).collect(Collectors.toSet());
//            entities = (Set) newHashSet.stream().map(l -> factory.createProjection(projectionClass, l)).collect(Collectors.toSet());
//            entities = newHashSet;
            entityMap.put(pred.toString(), entities);
        }
        
        return entities;
    }
}
