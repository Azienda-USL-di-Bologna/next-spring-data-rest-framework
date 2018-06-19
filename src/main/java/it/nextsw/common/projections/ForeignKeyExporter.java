package it.nextsw.common.projections;

import com.google.common.base.CaseFormat;
import it.nextsw.common.utils.EntityReflectionUtils;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import it.bologna.ausl.jenesisprojections.tools.ForeignKey;
import it.nextsw.common.utils.CommonUtils;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 *
 * @author gdm
 */
@Configuration
public class ForeignKeyExporter {

    @Autowired
    EntityReflectionUtils entityReflectionUtils;
    
    @Autowired
    CommonUtils commonUtils;

    @Value("${custom.mapping.url.root}")
    String customMappingBasePath;

    private String buildBaseUrl(String entityName) {
        HttpServletRequest currentRequest
                = ((ServletRequestAttributes) RequestContextHolder.
                        currentRequestAttributes()).getRequest();
 
        String hostName = commonUtils.getHostname(currentRequest);

//        System.out.println("get: " + currentRequest);
//        System.out.println("get: " + currentRequest.getServerName());
//        System.out.println("port: " + currentRequest.getServerPort());
//        System.out.println("port: " + currentRequest.getServerPort());
       
        
        String baseUrl = currentRequest.getScheme() + "://" + hostName + ":" + currentRequest.getServerPort() + customMappingBasePath + "/" + entityName.toLowerCase();

        return baseUrl;
    }

//    public ForeignKey toForeignKeyTemp(Object targetEntity) throws IOException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
//
//        ForeignKey fk = new ForeignKey();
//        Method getIdMethod = targetEntity.getClass().getDeclaredMethod("getId");
//        Object id = getIdMethod.invoke(targetEntity);
//
//        String entityClass;
//        if (targetEntity.getClass().getSuperclass().isAssignableFrom(Object.class)) {
//            entityClass = targetEntity.getClass().getSimpleName();
//        } else {
//            entityClass = targetEntity.getClass().getSuperclass().getSimpleName();
//        }
//
//        String url = buildBaseUrl(entityClass) + "/" + id.toString();
//
//        fk.setId(id.toString());
//        fk.setUrl(url);
//
//        return fk;
//    }
    public ForeignKey toForeignKey(String fieldName, Object SourceEntity) throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ServletException, EntityReflectionException {
        Object id = null;
        String targetEntityName = null;
        String url = null;

        Field field = SourceEntity.getClass().getDeclaredField(fieldName);
        if (field.getType().isAssignableFrom(Set.class) || field.getType().isAssignableFrom(List.class)) {
//            OneToMany annotation = field.getAnnotationsByType(OneToMany.class)[0];
//            String filterFieldName = annotation.mappedBy();
//            System.out.println("asfdfdafasf:  " + annotation.mappedBy());

            String fieldTypeName = field.getGenericType().getTypeName();

            System.out.println("class: " + fieldTypeName);
            String regex = "<([^<]+)>";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(fieldTypeName);
            if (matcher.find()) {
                String targetEntityFullName = matcher.group(1);
                Class<?> targetEntityClass = Class.forName(targetEntityFullName);
                String filterFieldName = getFilterFieldName(field, targetEntityClass);

                Field primaryKeyField = entityReflectionUtils.getPrimaryKeyField(SourceEntity.getClass());
                Method primaryKeyGetMethod = entityReflectionUtils.getPrimaryKeyGetMethod(SourceEntity);
                id = primaryKeyGetMethod.invoke(SourceEntity).toString();
                targetEntityName = targetEntityClass.getSimpleName().toLowerCase();
                url = String.format("%s?%s.%s=%s", buildBaseUrl(targetEntityName), filterFieldName, primaryKeyField.getName(), id);
//                String url = buildBaseUrl(targetEntityName) + "?" + filterFieldName + ".id=" + id.toString();
            } else {
                throw new ServletException("Le collection vanno dichiarate tipizzate");
            }
        } else {
            Method getFkMethod = SourceEntity.getClass().getMethod("get" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName));
            Object fkEntity = getFkMethod.invoke(SourceEntity);
            Class entityClass = getFkMethod.getReturnType();
//            Class trueEntityClass = entityReflectionUtils.getEntityFromProxyObject(fkEntity);
            Method fkPrimaryKeyGetMethod = entityReflectionUtils.getPrimaryKeyGetMethod(entityClass);

            targetEntityName = entityClass.getSimpleName().toLowerCase();
            if (fkEntity != null) {
                id = fkPrimaryKeyGetMethod.invoke(fkEntity).toString();
                url = String.format("%s/%s", buildBaseUrl(targetEntityName), id);
            }
        }

        ForeignKey fk = new ForeignKey(id, targetEntityName, url);
        return fk;
    }

    private String getFilterFieldName(Field field, Class targetEntityClass) {
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
}
