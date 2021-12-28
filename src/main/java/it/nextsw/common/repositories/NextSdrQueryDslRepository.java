package it.nextsw.common.repositories;

import com.fasterxml.jackson.databind.JsonNode;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.BooleanTemplate;
import com.querydsl.core.types.dsl.DatePath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import it.nextsw.common.controller.HibernateEntityInterceptor;
import it.nextsw.common.interceptors.NextSdrControllerInterceptor;
import it.nextsw.common.repositories.exceptions.InvalidFilterException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Column;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * QuerydslBinderCustomizer: customizza i filtri
 *
 * @author gdm
 * @param <E>
 * @param <ID>
 * @param <T>
 */
public interface NextSdrQueryDslRepository<E extends Object, ID extends Object, T extends EntityPath<?>>
        extends QuerydslBinderCustomizer<T>,
        QuerydslPredicateExecutor<E> {

   public Page<T> findAllNoCount(Predicate predicate, Pageable pageable);
    
    /**
     * per generare il Q per fare i filtri, si istanzia un oggetto del campo di
     * ricerca. Implemetando il metodo customize ci si gestisce i filtri come si
     * vuole.
     *
     *
     * @param bindings
     * @param entityPath
     */
    @Override
    @Nullable
    default void customize(QuerydslBindings bindings, T entityPath) {
        
        NextSdrControllerInterceptor.filterDescriptor.remove(); // Mi assicuro di reinizializzare la varibaile threadlocal
        Map<Path<?>, List<Object>> filterDescriptorMapaz = NextSdrControllerInterceptor.filterDescriptor.get();
        if (filterDescriptorMapaz == null) {
            filterDescriptorMapaz = new HashMap();
            NextSdrControllerInterceptor.filterDescriptor.set(filterDescriptorMapaz);
        }
        
        bindings.bind(Boolean.class).all((final Path<Boolean> path, final Collection<? extends Boolean> values) -> {
            
            Map<Path<?>, List<Object>> filterDescriptorMap = NextSdrControllerInterceptor.filterDescriptor.get();
            filterDescriptorMap.put(path, new ArrayList(values));
 
            final List<? extends Boolean> booleans = new ArrayList<>(values);
            Predicate res;
            
            BooleanPath booleanPath = (BooleanPath) path;
            
            if (values.isEmpty()) {
                res = Expressions.asBoolean(true).isTrue();
            } else if (values.size() == 1) {
                res = booleanPath.eq(booleans.get(0));
            } else {
                BooleanBuilder b = new BooleanBuilder();
                for (Boolean value : values) {
                    b = b.or(booleanPath.eq(value));
                }
                res = b;
            }
            return Optional.of(res);
        });
        
        bindings.bind(Long.class).all((final Path<Long> path, final Collection<? extends Long> values) -> {
            Map<Path<?>, List<Object>> filterDescriptorMap = NextSdrControllerInterceptor.filterDescriptor.get();
            filterDescriptorMap.put(path, new ArrayList(values));
 
            final List<? extends Long> numbers = new ArrayList<>(values);
            Predicate res;
            
            NumberPath longPath = (NumberPath) path;
            
            if (values.isEmpty()) {
                res = Expressions.asBoolean(true).isTrue();
            } else if (values.size() == 1) {
                res = longPath.eq(numbers.get(0));
            } else {
                BooleanBuilder b = new BooleanBuilder();
                for (Long value : values) {
                    b = b.or(longPath.eq(value));
                }
                res = b;
            }
            return Optional.of(res);
        });
        
        bindings.bind(Double.class).all((final Path<Double> path, final Collection<? extends Double> values) -> {
            Map<Path<?>, List<Object>> filterDescriptorMap = NextSdrControllerInterceptor.filterDescriptor.get();
            filterDescriptorMap.put(path, new ArrayList(values));
 
            final List<? extends Double> numbers = new ArrayList<>(values);
            Predicate res;
            
            NumberPath doublePath = (NumberPath) path;
            
            if (values.isEmpty()) {
                res = Expressions.asBoolean(true).isTrue();
            } else if (values.size() == 1) {
                res = doublePath.eq(numbers.get(0));
            } else {
                BooleanBuilder b = new BooleanBuilder();
                for (Double value : values) {
                    b = b.or(doublePath.eq(value));
                }
                res = b;
            }
            return Optional.of(res);
        });
        
        bindings.bind(Float.class).all((final Path<Float> path, final Collection<? extends Float> values) -> {
            Map<Path<?>, List<Object>> filterDescriptorMap = NextSdrControllerInterceptor.filterDescriptor.get();
            filterDescriptorMap.put(path, new ArrayList(values));
 
            final List<? extends Float> numbers = new ArrayList<>(values);
            Predicate res;
            
            NumberPath floatPath = (NumberPath) path;
            
            if (values.isEmpty()) {
                res = Expressions.asBoolean(true).isTrue();
            } else if (values.size() == 1) {
                res = floatPath.eq(numbers.get(0));
            } else {
                BooleanBuilder b = new BooleanBuilder();
                for (Float value : values) {
                    b = b.or(floatPath.eq(value));
                }
                res = b;
            }
            return Optional.of(res);
        });

        bindings.bind(LocalDate.class).all(
                (
                        final Path<LocalDate> path,
                        final Collection<? extends LocalDate> values) -> {
                    final List<? extends LocalDate> dates = new ArrayList<>(values);
                                       
                    Map<Path<?>, List<Object>> filterDescriptorMap = NextSdrControllerInterceptor.filterDescriptor.get();
                    filterDescriptorMap.put(path, new ArrayList(values));
                            
                    Predicate res;
                    if (values.size() == 1) {
                        DatePath datePath = (DatePath) path;
                        res = datePath.eq(dates.get(0));
                    } else if (dates.size() == 2) {
                        Collections.sort(dates);
                        DatePath datePath = (DatePath) path;
                        res = datePath.goe(dates.get(0)).and(datePath.loe(dates.get(1)));
                    } else {
                        res = Expressions.asBoolean(true).isTrue();
                    }
                    return Optional.of(res);
                });

        bindings.bind(LocalDateTime.class).all(
                (
                        final Path<LocalDateTime> path,
                        final Collection<? extends LocalDateTime> values) -> {
                    final List<? extends LocalDateTime> dates = new ArrayList<>(values);
                    
                    Map<Path<?>, List<Object>> filterDescriptorMap = NextSdrControllerInterceptor.filterDescriptor.get();
                    filterDescriptorMap.put(path, new ArrayList(values));
                    
                    Predicate res;
                    if (values.size() == 1) {
                        DateTimePath dateTimePath = (DateTimePath) path;
                        if (dates.get(0).toLocalDate().atTime(0, 0, 0).equals(LocalDateTime.of(9999, Month.JANUARY, 1, 0, 0, 0))) {
                            res = dateTimePath.isNull();
                        } else if (dates.get(0).toLocalDate().atTime(0, 0, 0).equals(LocalDateTime.of(9998, Month.JANUARY, 1, 0, 0, 0))) {
                            res = dateTimePath.isNotNull();
                        } else {
                            dateTimePath = (DateTimePath) path;
                            LocalDateTime startDate = dates.get(0).toLocalDate().atTime(0, 0, 0);
                            LocalDateTime endDate = startDate.plusDays(1);
                            res = dateTimePath.goe(startDate).and(dateTimePath.lt(endDate));
                        }
                    } else if (dates.size() == 2) {
                        Collections.sort(dates);
                        DateTimePath dateTimePath = (DateTimePath) path;
                        LocalDateTime startDate = dates.get(0).toLocalDate().atTime(0, 0, 0);
                        LocalDateTime endDate = dates.get(1).toLocalDate().atTime(0, 0, 0).plusDays(1);
                        res = dateTimePath.goe(startDate).and(dateTimePath.lt(endDate));
                    } else {
                        res = Expressions.asBoolean(true).isTrue();
                    }
                    return Optional.of(res);
                });
        
        bindings.bind(ZonedDateTime.class).all(
                (
                        final Path<ZonedDateTime> path,
                        final Collection<? extends ZonedDateTime> values) -> {
                    final List<? extends ZonedDateTime> dates = new ArrayList<>(values);
                    
                    Map<Path<?>, List<Object>> filterDescriptorMap = NextSdrControllerInterceptor.filterDescriptor.get();
                    filterDescriptorMap.put(path, new ArrayList(values));
                    
                    Predicate res;
                    if (values.size() == 1) {
                        DateTimePath dateTimePath = (DateTimePath) path;
                        if (dates.get(0).toLocalDate().atTime(0, 0, 0).equals(LocalDateTime.of(9999, Month.JANUARY, 1, 0, 0, 0))) {
                            res = dateTimePath.isNull();
                        } else if (dates.get(0).toLocalDate().atTime(0, 0, 0).equals(LocalDateTime.of(9998, Month.JANUARY, 1, 0, 0, 0))) {
                            res = dateTimePath.isNotNull();
                        } else {
                            dateTimePath = (DateTimePath) path;
                            ZonedDateTime startDate = dates.get(0).toLocalDate().atTime(0, 0, 0).atZone(dates.get(0).getZone());
                            ZonedDateTime endDate = startDate.plusDays(1);
                            res = dateTimePath.goe(startDate).and(dateTimePath.lt(endDate));
                        }
                    } else if (dates.size() == 2) {
                        Collections.sort(dates);
                        DateTimePath dateTimePath = (DateTimePath) path;
                        ZonedDateTime startDate = dates.get(0).toLocalDate().atTime(0, 0, 0).atZone(dates.get(0).getZone());
                        ZonedDateTime endDate = dates.get(1).toLocalDate().atTime(0, 0, 0).atZone(dates.get(0).getZone()).plusDays(1);
                        res = dateTimePath.goe(startDate).and(dateTimePath.lt(endDate));
                    } else {
                        res = Expressions.asBoolean(true).isTrue();
                    }
                    return Optional.of(res);
                });

        bindings.bind(Enum.class).first((path, value) -> {
            System.out.println("dentro");
            return null; //To change body of generated lambdas, choose Tools | Templates.
        });

        bindings.bind(String.class).all(
                (Path<String> path, Collection<? extends String> values) -> {
                    final List<? extends Object> strings = new ArrayList<>(values);
                    
                    Map<Path<?>, List<Object>> filterDescriptorMap = NextSdrControllerInterceptor.filterDescriptor.get();
                    filterDescriptorMap.put(path, new ArrayList(values));
                    
                    Predicate res;
                    try {
                        if (values.isEmpty()) {
                            res = Expressions.asBoolean(true).isTrue();
                        } else {
                            /* gestione dei campi tsvector:
                             * se sulla colonna dell'entità c'è l'annotazione Column e in columnDefinition contiene tsvector allora mi trovo in un campo tsvector
                             * in quel caso la ricerca viene effettuata secondo la metodologia di ricerca ts di postgres. Viene richiamata la funzione fts_match
                             *  che viene registrata creando in CustomDialect per hibernate (classe it.nextsw.common.dialect.CustomPostgresDialect).
                             * Per il corretto funzionamento è necessatio abilitare il CustomDialect aggiungendo la seguente riga nell'application.properties del progetto:
                             *  "spring.jpa.properties.hibernate.dialect=it.nextsw.common.dialect.CustomPostgresDialect"
                             */
                            String columDefinition = path.getAnnotatedElement().getAnnotation(Column.class).columnDefinition();
                            if (columDefinition != null && columDefinition.contains("tsvector")) {
                                BooleanExpression booleanTemplate = Expressions.booleanTemplate(
                                        String.format("FUNCTION('fts_match', italian, {0}, '%s')= true", ((String) strings.get(0)).replace("'", "''")),
                                        path
                                );
                                Map<String, String> rankQueryMap = HibernateEntityInterceptor.rankQueryObj.get();
                                if (rankQueryMap == null) {
                                    rankQueryMap = new HashMap();
                                    HibernateEntityInterceptor.rankQueryObj.set(rankQueryMap);
                                }
                                AnnotatedElement annotatedElement = path.getAnnotatedElement();
                                String buildingRankQuery = (String) strings.get(0);

                                if (values.size() > 1) {
                                    for (int i = 1; i < strings.size(); i++) {
                                        booleanTemplate = (booleanTemplate.or(Expressions.booleanTemplate(
                                                String.format("FUNCTION('fts_match', italian, {0}, '%s')= true", ((String) strings.get(i)).replace("'", "''")),
                                                path)));
                                        buildingRankQuery = buildingRankQuery + " " + ((String) strings.get(i));
                                    }
                                }
                                
                                if (Field.class.isAssignableFrom(annotatedElement.getClass())) {
                                    rankQueryMap.put(((Field)annotatedElement).getAnnotation(Column.class).name(), buildingRankQuery);
                                }
                                
                                res = booleanTemplate;
                            } else if (columDefinition != null && columDefinition.equalsIgnoreCase("text[]")) {
                                BooleanBuilder b = new BooleanBuilder();
                                BooleanExpression expression;
                                for (Object valueObj : values) {
                                    Object[] value = (Object[]) valueObj;
                                    if (!StringUtils.hasText((String) value[0])) {
                                        ArrayPath arrayPath = (ArrayPath) path;
                                        BooleanTemplate arrayIsEmpty = Expressions.booleanTemplate(
                                                "cardinality({0})=0", arrayPath
                                        );
                                        expression = arrayPath.isNull().or(arrayIsEmpty);
                                    } else {
                                        expression = Expressions.booleanTemplate(
                                                String.format("FUNCTION('array_operation', '%s', '%s', {0}, '%s')= true", org.apache.commons.lang3.StringUtils.join(value, ","), "text[]", "&&"),
                                                path
                                        );
                                    }
                                    b = b.or(expression);
                                }
                                res = b;
                            } //                            else if (columDefinition != null && columDefinition.contains("jsonb")) {
                            //                                if (strings.size() == 1) {
                            //                                    BooleanExpression booleanTemplate = Expressions.booleanTemplate(
                            //                                        String.format("FUNCTION('jsonb_match', {0}, '%s')= true", strings.get(0)), 
                            //                                        path
                            //                                    ); 
                            //                                res = booleanTemplate;
                            //                                } else {
                            //                                    BooleanBuilder b = new BooleanBuilder();
                            //                                    for (Object value: strings) {
                            //                                        BooleanExpression booleanTemplate = Expressions.booleanTemplate(
                            //                                            String.format("FUNCTION('jsonb_match', {0}, '%s')= true", value,
                            //                                            path
                            //                                            ));
                            //                                        b = b.or(booleanTemplate);
                            //                                    }
                            //                                    res = b;
                            //                                }
                            //                            } 
                            else {
                                StringPath stringPath = (StringPath) path;
                                if (values.size() == 1) {
                                    String string;
                                    if (strings.get(0).getClass().isEnum()) {
                                        string = ((Enum) strings.get(0)).toString();
                                        res = stringPath.eq(string);
                                    } else {
                                        string = (String) strings.get(0);
                                        res = getStringPredicate(string, stringPath);
                                    }
                                } else {
                                    BooleanBuilder b = new BooleanBuilder();
                                    for (Object value : values) {
                                        if (value.getClass().isEnum()) {
                                            String string = ((Enum) value).toString();
                                            b = b.or(stringPath.eq(string));
                                        } else {
                                            b = b.or(getStringPredicate((String) value, stringPath));
                                        }
                                    }
                                    res = b;
                                }
                            }
                        }
                        return Optional.of(res);

                    } catch (InvalidFilterException ex) {
                        return Optional.of(Expressions.asBoolean(true).isTrue());
                    }
                });

        bindings.bind(Integer.class).all(
                (Path<Integer> path, Collection<? extends Integer> values) -> {
                    final List<? extends Integer> numbers = new ArrayList<>(values);
                    
                    Map<Path<?>, List<Object>> filterDescriptorMap = NextSdrControllerInterceptor.filterDescriptor.get();
                    filterDescriptorMap.put(path, new ArrayList(values));
                    
                    Predicate res;
                    String columDefinition = path.getAnnotatedElement().getAnnotation(Column.class).columnDefinition();
                    if (columDefinition != null && columDefinition.equalsIgnoreCase("integer[]")) {
                        BooleanBuilder b = new BooleanBuilder();
                        BooleanExpression expression;
                        for (Object valueObj : values) {
                            Object[] value = (Object[]) valueObj;
                            if (value[0] == null) {
                                ArrayPath arrayPath = (ArrayPath) path;
                                BooleanTemplate arrayIsEmpty = Expressions.booleanTemplate(
                                        "cardinality({0})=0", arrayPath
                                );
                                expression = arrayPath.isNull().or(arrayIsEmpty);
                            } else {
                                expression = Expressions.booleanTemplate(
                                        String.format("FUNCTION('array_operation', '%s', '%s', {0}, '%s')= true", org.apache.commons.lang3.StringUtils.join(value, ","), "integer[]", "&&"),
                                        path
                                );
                            }
                            b = b.or(expression);
                        }
                        res = b;
                    } else {
                        NumberPath integerPath = (NumberPath) path;
                        if (values.isEmpty()) {
                            res = Expressions.asBoolean(true).isTrue();
                        } else if (values.size() == 1) {
                            // stratagemma per riuscire a filtrare per null.
                            // siccome, se passo null da errore perchè non è un numero, interpreto 999999999 come null e 999999998 come not null
                            if (numbers.get(0) == 999999999) {
                                res = integerPath.isNull();
                            } else if (numbers.get(0) == 999999998) {
                                res = integerPath.isNotNull();
                            } else {
                                res = integerPath.eq(numbers.get(0));
                            }
                        } else {
                            BooleanBuilder b = new BooleanBuilder();
                            for (Integer value : values) {
                                b = b.or(integerPath.eq(value));
                            }
                            res = b;
                        }
                    }

                    return Optional.of(res);
                });

        bindings.bind(JsonNode.class).all(
                (Path<JsonNode> path, Collection<? extends JsonNode> values) -> {
                    final List<? extends Object> strings = new ArrayList<>(values);
                    
                    Map<Path<?>, List<Object>> filterDescriptorMap = NextSdrControllerInterceptor.filterDescriptor.get();
                    filterDescriptorMap.put(path, new ArrayList(values));
                    
                    Predicate res;
                    try {
                        if (values.isEmpty()) {
                            res = Expressions.asBoolean(true).isTrue();
                        } else {
                            String columDefinition = path.getAnnotatedElement().getAnnotation(Column.class).columnDefinition();
                            if (columDefinition != null && columDefinition.contains("jsonb")) {
                                BooleanExpression booleanTemplate = Expressions.booleanTemplate(
                                    String.format("FUNCTION('jsonb_contains', {0}, '%s') = true", (String) strings.get(0)),
                                    path
                                );
                                
                                if (values.size() > 1) {
                                    for (int i = 1; i < strings.size(); i++) {
                                        booleanTemplate = (booleanTemplate.or(Expressions.booleanTemplate(
                                                String.format("FUNCTION('jsonb_contains', {0}, '%s') = true", ((String) strings.get(i))),
                                                path)));
                                    }
                                }
                                
                                res = booleanTemplate;
                            }  else {
                                res = Expressions.asBoolean(true).isTrue();;
                            }
                        }
                        return Optional.of(res);

                    } catch (Exception ex) {
                        return Optional.of(Expressions.asBoolean(true).isTrue());
                    }
                });
    }

    default StringOperation getStringOperation(String valueToParse) throws InvalidFilterException {
        StringOperation res;

        /**
         * esempio: nome=$equalsIgnorecase(NOME). I valori di filtro permessi
         * sono: contains, containsIgnoreCase, startsWith, startsWithIgnoreCase,
         * equals, equalsIgnoreCase
         */
        String regex = "\\$(.*?)\\((.*)\\)";
        Pattern r = Pattern.compile(regex);
        Matcher m = r.matcher(valueToParse);

        if (m.find()) {
            try {
                String operation = m.group(1);
                String value = m.group(2);
                res = new StringOperation(StringOperation.Operators.valueOf(operation), value);
            } catch (Exception ex) {
                throw new InvalidFilterException("filtro non valido", ex);
            }
        } else {
            throw new InvalidFilterException("filtro non valido");
        }

        return res;
    }

    default Predicate getStringPredicate(String valueToParse, StringPath stringPath) throws InvalidFilterException {

        BooleanExpression res;

        StringOperation stringOperation = getStringOperation(valueToParse);
      
        if (stringOperation.getOperator().equals(StringOperation.Operators.equals)) {
            res = stringPath.eq(stringOperation.getValue());
        } else {
            res = Expressions.booleanTemplate(
                String.format("FUNCTION('like', {0}, %s, %s) = true",  stringOperation.getValue(), stringOperation.getOperator().toString()),
                stringPath
            );
        }
//        switch (stringOperation.getOperator()) {
//            case contains:
//                res = stringPath.contains(stringOperation.getValue());
//                break;
//            case containsIgnoreCase:
//                res = stringPath.containsIgnoreCase(stringOperation.getValue());
//                break;
//            case startsWith:
//                res = stringPath.startsWith(stringOperation.getValue());
//                break;
//            case startsWithIgnoreCase:
//                res = stringPath.startsWithIgnoreCase(stringOperation.getValue());
//                break;
//            case equals:
//                res = stringPath.eq(stringOperation.getValue());
//                break;
//            case equalsIgnoreCase:
//                res = stringPath.equalsIgnoreCase(stringOperation.getValue());
//                break;
//            default:
//                throw new InvalidFilterException(String.format("operatore %s non valido", stringOperation.getOperator()));
//        }
        return res;
    }

}
