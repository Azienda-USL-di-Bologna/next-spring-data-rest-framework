package it.nextsw.common.utils;

import com.google.common.base.CaseFormat;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class EntityReflectionUtils {

    private String projectionPackage = "it.bologna.ausl.shalbo.entities.projections";
    private String generatedProjectionPackage = "it.bologna.ausl.shalbo.entities.projections.generated";

    public Class<?> getProjectionClass(String projection) {
        Class<?> projectionClass = null;
        try {
            projectionClass = Class.forName(projectionPackage + "." + projection);
        } catch (ClassNotFoundException ex) {
            try {
                projectionClass = Class.forName(generatedProjectionPackage + "." + projection);
            } catch (ClassNotFoundException subex) {
                // loggare errore
            }
        }
        return projectionClass;
    }

    public Method getPrimaryKeySetMethod(Object entity) throws NoSuchMethodException {
        return getPrimaryKeySetMethod(entity.getClass());
    }

    public Method getPrimaryKeySetMethod(Class entityClass) throws NoSuchMethodException {
        Field primaryKeyField = getPrimaryKeyField(entityClass);
        Class fieldType = primaryKeyField.getType();
        return entityClass.getMethod("set" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, primaryKeyField.getName()), fieldType);
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

    public boolean isEntityClass(Class classz) {
        java.lang.annotation.Annotation annotation = classz.getAnnotation(javax.persistence.Entity.class);
        //System.out.println(annotation);
        return annotation != null;
    }

    /**
     * Torna la classe Entity vera e propria a partire degli oggetti proxy
     * generati da Spring. Chiama la getSuperClass fino a che non ottiene la
     * classe Entity
     *
     * @param proxyEntity
     * @return l'oggetto entity vero e proprio a partire degli oggetti proxy
     * generati da Spring
     */
    public Class getEntityFromProxyObject(Object proxyEntity) throws EntityReflectionException {
        Class<?> classz = proxyEntity.getClass();
        do {
            if (isEntityClass(classz)) {
                return classz;
            }
            classz = classz.getSuperclass();
        } while (!classz.isAssignableFrom(Object.class));
        throw new EntityReflectionException("l'oggetto passato non deriva da un'Entity");
    }

    public Class getDefaultProjection(Object repository) throws EntityReflectionException {
        Class classz = repository.getClass();
        AnnotatedType[] annotatedInterfaces = classz.getAnnotatedInterfaces();
        for (AnnotatedType annotatedType : annotatedInterfaces) {
            try {
                classz = Class.forName(annotatedType.getType().getTypeName());
            } catch (ClassNotFoundException ex) {
                throw new EntityReflectionException("l'oggetto passato non è un repository o non ha l'annotazione RepositoryRestResource");
            }
            do {
                Annotation annotation = classz.getAnnotation(RepositoryRestResource.class);
                if (annotation != null) {
                    RepositoryRestResource repositoryRestResourceAnnotation = (RepositoryRestResource) annotation;
                    return repositoryRestResourceAnnotation.excerptProjection();
                } else {
                    classz = classz.getSuperclass();
                }
            } while (!classz.isAssignableFrom(Object.class));
        }
        throw new EntityReflectionException("l'oggetto passato non è un repository o non ha l'annotazione RepositoryRestResource");
    }
}
