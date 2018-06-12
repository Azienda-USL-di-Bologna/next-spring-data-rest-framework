package it.nextsw.common.repositories;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.DatePath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringPath;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

        bindings.bind(LocalDate.class).all(
                (
                        final Path<LocalDate> path,
                        final Collection<? extends LocalDate> values) -> {
                    final List<? extends LocalDate> dates = new ArrayList<>(values);
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
                    Predicate res;
                    if (values.size() == 1) {
                        DateTimePath dateTimePath = (DateTimePath) path;
                        LocalDateTime startDate = dates.get(0).toLocalDate().atTime(0, 0, 0);
                        LocalDateTime endDate = startDate.plusDays(1);
                        res = dateTimePath.goe(startDate).and(dateTimePath.lt(endDate));
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

        bindings.bind(String.class).all(
                (StringPath path, Collection<? extends String> values) -> {
                    final List<? extends String> strings = new ArrayList<>(values);
                    Predicate res;
                    if (values.isEmpty()) {
                        res = Expressions.asBoolean(true).isTrue();
                    } else if (values.size() == 1) {
                        res = path.containsIgnoreCase(strings.get(0));
                    } else {
                        BooleanBuilder b = new BooleanBuilder();
                        values.forEach(value -> b.or(path.containsIgnoreCase(value)));
                        res = b;
                    }
                    return Optional.of(res);
                });
    }
}
