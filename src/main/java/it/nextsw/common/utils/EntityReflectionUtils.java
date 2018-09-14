package it.nextsw.common.utils;

import com.google.common.base.CaseFormat;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author gdm
 */
@Component
public class EntityReflectionUtils {

    public Method getPrimaryKeySetMethod(Object entity) throws NoSuchMethodException {
        return getPrimaryKeySetMethod(entity.getClass());
    }

    public Method getPrimaryKeySetMethod(Class entityClass) throws NoSuchMethodException {
        Field primaryKeyField = getPrimaryKeyField(entityClass);
        Class fieldType = primaryKeyField.getType();
        return entityClass.getMethod("set" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, primaryKeyField.getName()), fieldType);
    }
    
    public boolean hasSerialPrimaryKey(Class entityClass) throws NoSuchMethodException {
        Field primaryKeyField = getPrimaryKeyField(entityClass);
        GeneratedValue generatedValueAnnotation = primaryKeyField.getAnnotation(GeneratedValue.class);
        return generatedValueAnnotation != null;
    }

    public Method getPrimaryKeyGetMethod(Object entity) throws NoSuchMethodException {
        return getPrimaryKeyGetMethod(entity.getClass());
    }

    public Method getPrimaryKeyGetMethod(Class entityClass) throws NoSuchMethodException {
        Field primaryKeyField = getPrimaryKeyField(entityClass);
        return entityClass.getMethod("get" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, primaryKeyField.getName()));
    }

    public Field getPrimaryKeyField(Class entityClass) {
        List<Field> declaredFields = new ArrayList(Arrays.asList(entityClass.getDeclaredFields()));
        Class superclass = entityClass.getSuperclass();
        while (superclass != null) {
            declaredFields.addAll(new ArrayList(Arrays.asList(superclass.getDeclaredFields())));
            superclass = superclass.getSuperclass();
        }
        Field res = null;
        for (Field declaredField : declaredFields) {
            if (declaredField.getAnnotation(Id.class) != null) {
                res = declaredField;
                break;
            }
        }
        return res;
    }

    public boolean isForeignKeyField(Field field) {
        return field.getAnnotation(OneToMany.class) != null
                || field.getAnnotation(ManyToOne.class) != null
                || field.getAnnotation(OneToOne.class) != null
                || field.getAnnotation(ManyToMany.class) != null;
    }

    public boolean isRepositoryClass(Class classz) {
        java.lang.annotation.Annotation annotation = classz.getAnnotation(RepositoryRestResource.class);
        //System.out.println(annotation);
        return annotation != null;
    }

    public boolean isEntityClassFromProxyObject(Class classz) {
        java.lang.annotation.Annotation annotation = null;
        Class superclass = classz;
        while (superclass != null && annotation == null) {
            annotation = superclass.getAnnotation(javax.persistence.Entity.class);
            superclass = superclass.getSuperclass();
        }
        return annotation != null;
    }

    public boolean isEntityClass(Class classz) {
        java.lang.annotation.Annotation annotation = classz.getAnnotation(javax.persistence.Entity.class);
        return annotation != null;
    }

    /**
     * Torna la classe Entity vera e propria a partire degli oggetti proxy
     * generati da Spring. Chiama la getSuperClass fino a che non ottiene la
     * classe Entity
     *
     * @param proxyEntity
     * @return la classe entity vero e proprio a partire dalle classi proxy
     * generate da Spring
     * @throws it.nextsw.common.utils.exceptions.EntityReflectionException
     */
    public Class getEntityFromProxyObject(Object proxyEntity) throws EntityReflectionException {
        return getEntityFromProxyClass(proxyEntity.getClass());
    }

    /**
     * Torna la classe Entity vera e propria a partire dalle classi proxy
     * generati da Spring. Chiama la getSuperClass fino a che non ottiene la
     * classe Entity
     *
     * @param proxyEntityClass
     * @return la classe entity vero e proprio a partire dalle classi proxy
     * generate da Spring
     * @throws it.nextsw.common.utils.exceptions.EntityReflectionException
     */

    public Class getEntityFromProxyClass(Class<?> proxyEntityClass) throws EntityReflectionException {
        Class<?> classz = proxyEntityClass;
        do {
            if (isEntityClass(classz)) {
                return classz;
            }
            classz = classz.getSuperclass();
        } while (!classz.isAssignableFrom(Object.class));
        throw new EntityReflectionException("l'oggetto passato non deriva da un'Entity");
    }

    /**
     *
     * @param repository
     * @return
     * @throws EntityReflectionException
     */
    public Class getDefaultProjection(Object repository) throws EntityReflectionException {
        Class classz = repository.getClass();
        NextSdrRepository nextSdrRepository = null;
        try {
            nextSdrRepository = (NextSdrRepository) EntityReflectionUtils.getFirstAnnotationOverHierarchy(classz, NextSdrRepository.class);
        } catch (ClassNotFoundException e) {
            throw new EntityReflectionException(e);
        }
        return nextSdrRepository.defaultProjection();


//        AnnotatedType[] annotatedInterfaces = classz.getAnnotatedInterfaces();
//        for (AnnotatedType annotatedType : annotatedInterfaces) {
//            try {
//                classz = Class.forName(annotatedType.getType().getTypeName());
//            } catch (ClassNotFoundException ex) {
//                throw new EntityReflectionException("l'oggetto passato non è un repository o non ha l'annotazione RepositoryRestResource");
//            }
//            do {
//                Annotation annotation = classz.getAnnotation(RepositoryRestResource.class);
//                if (annotation != null) {
//                    RepositoryRestResource repositoryRestResourceAnnotation = (RepositoryRestResource) annotation;
//                    return repositoryRestResourceAnnotation.excerptProjection();
//                } else {
//                    classz = classz.getSuperclass();
//                }
//            } while (!classz.isAssignableFrom(Object.class));
//        }
//        throw new EntityReflectionException("l'oggetto passato non è un repository o non ha l'annotazione RepositoryRestResource");
    }

    /**
     *
     * @param objectClass
     * @param annotationClass
     * @return
     */
    public static Annotation getFirstAnnotationOverHierarchy(Class objectClass, Class annotationClass) throws ClassNotFoundException {
        AnnotatedType[] annotatedInterfaces = objectClass.getAnnotatedInterfaces();
        for (AnnotatedType annotatedType : annotatedInterfaces) {
                objectClass = (Class) annotatedType.getType();
            do {
                Annotation annotation = objectClass.getAnnotation(annotationClass);
                if (annotation != null) {
                    return annotation;
                } else {
                    objectClass = objectClass.getSuperclass();
                }
            } while (!objectClass.isAssignableFrom(Object.class));
        }
        return null;
    }
    
    public String getFilterFieldName(Field field, Class targetEntityClass) {
        String filterFieldName = null;

        // se l'annotazione è OneToMany allora il filterFieldName si ottiene dal mappedBy
        OneToMany oneToManyAnnotation = field.getAnnotationsByType(OneToMany.class)[0];
        if (oneToManyAnnotation != null) {
            filterFieldName = oneToManyAnnotation.mappedBy();
        } else {
            // se l'annotazione è ManyToMany ci sono 2 casi, se c'è il mappedBy, allora il filterFieldName si ottiene da esso
            ManyToMany manyToManyAnnotation = field.getAnnotationsByType(ManyToMany.class)[0];
            if (manyToManyAnnotation != null) {
                if (manyToManyAnnotation.mappedBy() != null && !manyToManyAnnotation.mappedBy().isEmpty()) {
                    filterFieldName = manyToManyAnnotation.mappedBy();
                } /**
                 * altrimenti devo andare a cercare nell'entità a cui punta la
                 * Foreign Key il campo che ha l'annotazione ManyToMany con
                 * mappedBy = al nome del campo dell'entità Source. una volta
                 * trovato il filterFieldName sarà il nome del campo
                 *
                 */
                else {
                    Field[] TargetClassFields = targetEntityClass.getDeclaredFields();
                    for (Field targetField : TargetClassFields) {
                        ManyToMany annotation = targetField.getAnnotationsByType(ManyToMany.class)[0];
                        if (annotation.mappedBy().equals(field.getName())) {
                            filterFieldName = targetField.getName();
                            break;
                        }
                    }
                }
            }
        }
        return filterFieldName;
    }

    /**
     * Torna "true" se il campo dell'entità passato ha settato orphanRemoval = true sull'annotazione OneToMany o OneToOne
     * "false" in tutti gli altri casi o se c'è un qualsiasi errore
     * @param entityField il campo FK dell'entità
     * @return 
     */
    public boolean hasOrphanRemoval(Field entityField) {
        try {
            OneToMany oneToManyAnnotation = entityField.getAnnotation(javax.persistence.OneToMany.class);
            if (oneToManyAnnotation != null)
                return oneToManyAnnotation.orphanRemoval();
            else {
                OneToOne oneToOneAnnotation = entityField.getAnnotation(javax.persistence.OneToOne.class);
                if (oneToOneAnnotation != null)
                    return oneToOneAnnotation.orphanRemoval();
            }
        }
        catch (Exception ex) {}
        return false;
    }

    /**
     * Torna l'entità alla quale il repository fa riferimento. 
     * Il repository deve estendere la classe NextSdrQueryDslRepository.
     * @param repository
     * @return l'entità alla quale il repository fa riferimento, null se l'entità non viene trovata.
     */
    public Class getEntityClassFromRepository(Object repository) {
        Type[] genericInterfaces = repository.getClass().getGenericInterfaces();
        for (Type type : genericInterfaces) {
            if (NextSdrQueryDslRepository.class.isAssignableFrom((Class<?>) type)) {
                Class<NextSdrQueryDslRepository> repositoyInterface = (Class<NextSdrQueryDslRepository>) type;
                Type[] exendendInterfaces = repositoyInterface.getGenericInterfaces();
                for (Type exendendInterface : exendendInterfaces) {
                    ParameterizedType exendendInterfaceParametrized = (ParameterizedType) exendendInterface;
                    if (NextSdrQueryDslRepository.class.isAssignableFrom((Class<?>) exendendInterfaceParametrized.getRawType())) {
                        Class entityType = (Class) exendendInterfaceParametrized.getActualTypeArguments()[0];
                        return entityType;
                    }
                }
            }
        }
        return  null;
    }
    
}
