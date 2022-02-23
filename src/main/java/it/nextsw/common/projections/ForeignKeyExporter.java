package it.nextsw.common.projections;

import com.google.common.base.CaseFormat;
import it.nextsw.common.annotations.NextSdrRepository;
import it.nextsw.common.repositories.NextSdrQueryDslRepository;
import it.nextsw.common.utils.CommonUtils;
import it.nextsw.common.utils.EntityReflectionUtils;
import it.nextsw.common.utils.ForeignKey;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 *
 * @author gdm
 */
@Component
public class ForeignKeyExporter {

    @Autowired
    CommonUtils commonUtils;

    /**
     * mappa dei repository
     */
    @Autowired
    @Qualifier(value = "customRepositoryEntityMap")
    protected Map<String, NextSdrQueryDslRepository> customRepositoryEntityMap;

    private String buildUrl(Class targetEntityClass) throws ClassNotFoundException {
        HttpServletRequest currentRequest
                = ((ServletRequestAttributes) RequestContextHolder.
                        currentRequestAttributes()).getRequest();
        String url = null;
        NextSdrQueryDslRepository targetEntityRepository = customRepositoryEntityMap.get(targetEntityClass.getCanonicalName());
        if (targetEntityRepository != null) {
            NextSdrRepository annotation = EntityReflectionUtils.getFirstAnnotationOverHierarchy(targetEntityRepository.getClass(), NextSdrRepository.class);
            String path = commonUtils.resolvePlaceHolder(annotation.repositoryPath());
            String hostName = commonUtils.getHostname(currentRequest);
            url = currentRequest.getScheme() + "://" + hostName + ":" + currentRequest.getServerPort() + path;
        }

        return url;
    }

    public ForeignKey toForeignKey(String fieldName, Object SourceEntity) throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ServletException, EntityReflectionException {
        Object id = null;
        String targetEntityName = null;
        String fullUrl = null;

        // esempio idAzienda
        Field field = EntityReflectionUtils.getEntityFromProxyObject(SourceEntity).getDeclaredField(fieldName);
//        Field field = SourceEntity.getClass().getDeclaredField(fieldName);

        /**
         * può essere una Collection o un 'Entità. esempio List<Pec>
         */
        if (Collection.class.isAssignableFrom(field.getType())) {
//            OneToMany annotation = field.getAnnotationsByType(OneToMany.class)[0];
//            String filterFieldName = annotation.mappedBy();
//            System.out.println("asfdfdafasf:  " + annotation.mappedBy());

            String fieldTypeName = field.getGenericType().getTypeName();

//            System.out.println("class: " + fieldTypeName);
            // prendiamo la classe racchiusa tra '<>'
            String regex = "<([^<]+)>";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(fieldTypeName);
            if (matcher.find()) {
                String targetEntityFullName = matcher.group(1);
                // qui ho la classe di Pec (se si considera l'esempio con le Pec)
                Class<?> targetEntityClass = Class.forName(targetEntityFullName);
                /**
                 * il nuovo URL deve avere idAzienda.id=5, quindi generare il
                 * link
                 */
                String filterFieldName = EntityReflectionUtils.getFilterFieldName(field, targetEntityClass);

                if (filterFieldName != null) {
                    Field primaryKeyField = EntityReflectionUtils.getPrimaryKeyField(SourceEntity.getClass());
                    Method primaryKeyGetMethod = EntityReflectionUtils.getPrimaryKeyGetMethod(SourceEntity);
                    id = primaryKeyGetMethod.invoke(SourceEntity);
                    targetEntityName = targetEntityClass.getSimpleName().toLowerCase();
                    String buildedUrl = buildUrl(targetEntityClass);
                    if (buildedUrl != null) {
                        fullUrl = String.format("%s?%s.%s=%s", buildedUrl, filterFieldName, primaryKeyField.getName(), id);
                    }
                }
//                String url = buildBaseUrl(targetEntityName) + "?" + filterFieldName + ".id=" + id.toString();
            } else {
                throw new ServletException("Le collection vanno dichiarate tipizzate");
            }
        } else {
            // getFkMethod esempio è getIdAzienda
            Method getFkMethod = SourceEntity.getClass().getMethod("get" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName));
            Object fkEntity = getFkMethod.invoke(SourceEntity);
            // prende il tipo di ritorno del metodo, sarebbe Azienda
            Class entityClass = getFkMethod.getReturnType();
//            Class trueEntityClass = entityReflectionUtils.getEntityFromProxyObject(fkEntity);
            // dal tipo di ritorno si va a prendere la getPrimaryKey
            Method fkPrimaryKeyGetMethod = EntityReflectionUtils.getPrimaryKeyGetMethod(entityClass);

            // qui nell'esempio con l'azienda sarà Azienda
            targetEntityName = entityClass.getSimpleName().toLowerCase();
            // se ha un valore l'oggetto, allora si va a calcolare l'id, invocando getId() su azienda
            if (fkEntity != null) {
                id = fkPrimaryKeyGetMethod.invoke(fkEntity);
                fullUrl = String.format("%s/%s", buildUrl(entityClass), id);
            }
        }

        ForeignKey fk = new ForeignKey(id, targetEntityName, fullUrl);
        return fk;
    }
}
