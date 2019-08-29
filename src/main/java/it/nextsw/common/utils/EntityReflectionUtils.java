package it.nextsw.common.utils;

import com.google.common.base.CaseFormat;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

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
 * Classe di utilità di reflection per Entità
 */
public class EntityReflectionUtils {

    public static Method getPrimaryKeySetMethod(Object entity) throws NoSuchMethodException {
        return getPrimaryKeySetMethod(entity.getClass());
    }

    public static Method getPrimaryKeySetMethod(Class entityClass) throws NoSuchMethodException {
        Field primaryKeyField = getPrimaryKeyField(entityClass);
        Class fieldType = primaryKeyField.getType();
        return entityClass.getMethod("set" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, primaryKeyField.getName()), fieldType);
    }
    
    public static boolean hasSerialPrimaryKey(Class entityClass) throws NoSuchMethodException {
        Field primaryKeyField = getPrimaryKeyField(entityClass);
        GeneratedValue generatedValueAnnotation = primaryKeyField.getAnnotation(GeneratedValue.class);
        return generatedValueAnnotation != null;
    }

    public static Method getPrimaryKeyGetMethod(Object entity) throws NoSuchMethodException {
        return getPrimaryKeyGetMethod(entity.getClass());
    }

    public static Method getPrimaryKeyGetMethod(Class entityClass) throws NoSuchMethodException {
        Field primaryKeyField = getPrimaryKeyField(entityClass);
        return getGetMethod(entityClass,primaryKeyField.getName());
    }

    public static Field getPrimaryKeyField(Class entityClass) {
        return getFieldFromAnnotation(entityClass, Id.class);
    }
    
    public static Field getVersionField(Class entityClass) {
        return getFieldFromAnnotation(entityClass, Version.class);
    }
    
    public static Field getFieldFromAnnotation(Class classz, Class annotationClass) {
        if (!annotationClass.isAnnotation()) {
            throw new RuntimeException(String.format("annotationClass deve essere un'annotazione"));
        }
        
        List<Field> declaredFields = new ArrayList(Arrays.asList(classz.getDeclaredFields()));
        Class superclass = classz.getSuperclass();
        while (superclass != null) {
            declaredFields.addAll(new ArrayList(Arrays.asList(superclass.getDeclaredFields())));
            superclass = superclass.getSuperclass();
        }
        Field res = null;
        for (Field declaredField : declaredFields) {
            if (declaredField.getAnnotation(annotationClass) != null) {
                res = declaredField;
                break;
            }
        }
        return res;
    }

    public static boolean isForeignKeyField(Field field) {
        return field.getAnnotation(OneToMany.class) != null
                || field.getAnnotation(ManyToOne.class) != null
                || field.getAnnotation(OneToOne.class) != null
                || field.getAnnotation(ManyToMany.class) != null;
    }

    public static boolean isRepositoryClass(Class classz) {
        java.lang.annotation.Annotation annotation = classz.getAnnotation(RepositoryRestResource.class);
        //System.out.println(annotation);
        return annotation != null;
    }

    public static boolean isEntityClassFromProxyObject(Class classz) {
        java.lang.annotation.Annotation annotation = null;
        Class superclass = classz;
        while (superclass != null && annotation == null) {
            annotation = superclass.getAnnotation(javax.persistence.Entity.class);
            superclass = superclass.getSuperclass();
        }
        return annotation != null;
    }

    public static boolean isEntityClass(Class classz) {
        java.lang.annotation.Annotation annotation = classz.getAnnotation(javax.persistence.Entity.class);
        return annotation != null;
    }
    
    public static boolean isColumnOrVersionOrFkField(Field field) {
        java.lang.annotation.Annotation columnAnnotation = field.getAnnotation(Column.class);
        return columnAnnotation != null || isVersionField(field) || EntityReflectionUtils.isForeignKeyField(field);
    }
    
    public static boolean isVersionField(Field field) {
        java.lang.annotation.Annotation versionAnnotation = field.getAnnotation(Version.class);
        return versionAnnotation != null;
    }

    /**
     * Torna la classe Entity vera e propria a partire degli oggetti proxy
     * generati da Spring. Chiama la getSuperClass fino a che non ottiene la
     * classe Entity
     *
     * @param proxyEntity
     * @return la classe entities vero e proprio a partire dalle classi proxy
     * generate da Spring
     * @throws it.nextsw.common.utils.exceptions.EntityReflectionException
     */
    public static Class getEntityFromProxyObject(Object proxyEntity) throws EntityReflectionException {
        return getEntityFromProxyClass(proxyEntity.getClass());
    }

    /**
     * Torna la classe Entity vera e propria a partire dalle classi proxy
     * generati da Spring. Chiama la getSuperClass fino a che non ottiene la
     * classe Entity
     *
     * @param proxyEntityClass
     * @return la classe entities vero e proprio a partire dalle classi proxy
     * generate da Spring
     * @throws it.nextsw.common.utils.exceptions.EntityReflectionException
     */

    public static Class getEntityFromProxyClass(Class<?> proxyEntityClass) throws EntityReflectionException {
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
    public static Class getDefaultProjection(Object repository) throws EntityReflectionException {
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
     * @param <T>
     * @param objectClass
     * @param annotationClass
     * @return
     */
    public static <T extends Annotation> T getFirstAnnotationOverHierarchy(Class objectClass, Class<T> annotationClass) throws ClassNotFoundException {
        AnnotatedType[] annotatedInterfaces = objectClass.getAnnotatedInterfaces();
        for (AnnotatedType annotatedType : annotatedInterfaces) {
                objectClass = (Class) annotatedType.getType();
            do {
                Annotation annotation = objectClass.getAnnotation(annotationClass);
                if (annotation != null) {
                    return (T) annotation;
                } else {
                    objectClass = objectClass.getSuperclass();
                }
            } while (!objectClass.isAssignableFrom(Object.class));
        }
        return null;
    }
    
    public static String getFilterFieldName(Field field, Class targetEntityClass) {
        String filterFieldName = null;

        // se l'annotazione è OneToMany allora il filterFieldName si ottiene dal mappedBy
        OneToMany oneToManyAnnotation = Arrays.stream(field.getAnnotationsByType(OneToMany.class)).findFirst().orElse(null);
        if (oneToManyAnnotation != null) {
            filterFieldName = oneToManyAnnotation.mappedBy();
        } else {
            // se l'annotazione è ManyToMany ci sono 2 casi, se c'è il mappedBy, allora il filterFieldName si ottiene da esso
            ManyToMany manyToManyAnnotation = Arrays.stream(field.getAnnotationsByType(ManyToMany.class)).findFirst().orElse(null);
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
                    Field[] targetClassFields = targetEntityClass.getDeclaredFields();
                    for (Field targetField : targetClassFields) {
                        ManyToMany annotation = Arrays.stream(targetField.getAnnotationsByType(ManyToMany.class)).findFirst().orElse(null);
                        if (annotation != null && annotation.mappedBy()!= null &&
                                annotation.mappedBy().equals(field.getName())) {
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
    public static boolean hasOrphanRemoval(Field entityField) {
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
    public static Class getEntityClassFromRepository(Object repository) {
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

    /**
     * Reperimento del metodo set di un particolare campo, di una particolare classe
     *
     * @param entityClass
     * @param fieldName
     * @return
     */
    public static Method getSetMethod(Class entityClass, String fieldName)  {
        Method result = ReflectionUtils.findMethod(entityClass, "set" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName), null);
        if (result != null)
            return result;
        else
            throw new RuntimeException(String.format("metodo set per il campo %s non trovato", fieldName));
    }

    /**
     * Reperimento del metodo get di un particolare campo, di una particolare classe
     *
     * @param entityClass
     * @param fieldName
     * @return
     */
    public static Method getGetMethod(Class entityClass, String fieldName)  {

        Method result = ReflectionUtils.findMethod(entityClass, "get" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName));
        if (result == null){
            result = ReflectionUtils.findMethod(entityClass, "is" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName));
            if (result!=null)
                return result;
            else
                throw new RuntimeException(String.format("metodo get per il campo %s non trovato", fieldName));
        } else
            return result;
    }

    /**
     * Il metodo ritorna il field della classe o di una delle sue superclassi
     *
     * @param entityClass la classe su cui cercare il field
     * @param fieldName il nome del field
     * @return
     * @throws RuntimeException se non trova nessun field col nome passato
     */

    public static Field getDeclaredField(Class entityClass, String fieldName) throws RuntimeException {
        Field result = ReflectionUtils.findField(entityClass, fieldName);
        if (result != null)
            return result;
        else
            throw new RuntimeException(String.format("Field il campo %s non trovato", fieldName));
    }

}
