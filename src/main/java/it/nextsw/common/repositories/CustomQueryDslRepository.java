package it.nextsw.common.repositories;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DatePath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.core.types.dsl.TemporalExpression;
import com.querydsl.core.types.dsl.TimePath;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;

/**
 *
 * @author gdm
 * @param <E>
 * @param <ID>
 * @param <T>
 */
public interface CustomQueryDslRepository<E extends Object, ID extends Object, T extends EntityPath<?>>
        extends QuerydslBinderCustomizer<T>,
        QuerydslPredicateExecutor<E> {

    @Override
    default void customize(QuerydslBindings bindings, T entityPath) {

        bindings.bind(java.util.Date.class).all(
                (
                        final Path<java.util.Date> path,
                        final Collection<? extends java.util.Date> values) -> {
                    final List<? extends java.util.Date> dates = new ArrayList<>(values);
                    Collections.sort(dates);

                    if (dates.size() != 2) {
                        return Optional.of(Expressions.asBoolean(true).isTrue());
                    } else {
                        TemporalType temporalType = path.getAnnotatedElement().getAnnotation(Temporal.class).value();
                        Optional<Predicate> res;
                        switch (temporalType) {
                            case DATE:
                                DatePath datePath = (DatePath) path;
                                res = Optional.of(datePath.between(dates.get(0), dates.get(1)));
                                break;
                            case TIMESTAMP:
                                DateTimePath dateTimePath = (DateTimePath) path;
                                res = Optional.of(dateTimePath.between(dates.get(0), dates.get(1)));
                                break;
                            case TIME:
                                TimePath timePath = (TimePath) path;
                                res = Optional.of(timePath.between(dates.get(0), dates.get(1)));
                                break;
                            default:
                                res = Optional.of(Expressions.asBoolean(true).isTrue());
                        }
                        return res;
                    }
                });

//        bindings.bind(java.util.Date.class).firstOptional(
////                (path, value) -> {
////            System.out.println(value);
////            DateTimePath<java.util.Date> patha = (DateTimePath<java.util.Date>)path;
//////                    return patha.eq(value.get());
////                     return Optional.of(patha.eq(value.get()));
////                }
////        );
//                (DateTimePath<java.util.Date> path, Object value) -> {
//                    System.out.println(value);
//                    
//                    Optional<? extends Date> value1 = (Optional<? extends Date>) value;
//                    
//                    return Optional.of(path.eq(value1.get()));
//                }
//        );
        bindings.bind(java.util.Date.class).first((Path<Date> path, Date value) -> {
            System.out.println("date: " + value);
            System.out.println("path: " + path);
            TemporalType temporalType = path.getAnnotatedElement().getAnnotation(Temporal.class).value();
            Predicate res;
            switch (temporalType) {
                case DATE:
                    DatePath datePath = (DatePath) path;
                    res = datePath.eq(value);
                    break;
                case TIMESTAMP:
                    DateTimePath dateTimePath = (DateTimePath) path;
                    Calendar c = Calendar.getInstance();
                    c.setTime(value);
                    c.set(Calendar.HOUR_OF_DAY, 0);
                    c.set(Calendar.MINUTE, 0);
                    c.set(Calendar.SECOND, 0);
                    Date dateMin = c.getTime();
                    System.out.println("date_min: " + dateMin);

                    c.add(Calendar.DAY_OF_MONTH, 1);
                    Date dateMax = c.getTime();
                    System.out.println("date_max: " + dateMax);

                    res = dateTimePath.goe(dateMin).and(dateTimePath.lt(dateMax));
                    break;
                case TIME:
                    TimePath timePath = (TimePath) path;
                    res = timePath.eq(value);
                    break;
                default:
                    res = Expressions.asBoolean(true).isTrue();
            }
            return res;
        });
//                    (Path p, java.util.Date value) -> {
//                        System.out.println("date: " + value);
//                        BooleanExpression eq = p.eq(value);
//                        return eq;
//                    }
//                );

        bindings.bind(String.class).all(
                (StringPath path, Collection<? extends String> values) -> {

                    final List<? extends String> valuesList = new ArrayList<>(values);

                    return Optional.of(path.in(valuesList));
                });

        bindings.bind(String.class).first((StringPath path, String value) -> path.containsIgnoreCase(value));
    }
}
