package it.nextsw.common.repositories;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPQLQuery;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.QuerydslJpaPredicateExecutor;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.util.Assert;

/**
 *
 * @author gdm
 * @param <E>
 * @param <ID>
 * @param <T>
 */
//@Repository
public class NextQuerydslJpaPredicateExecutorImpl<E extends Object, ID extends Object, T extends EntityPath<?>> 
        implements NextSdrQueryDslRepository {

    private final QuerydslJpaPredicateExecutor querydslPredicateExecutor;

    public NextQuerydslJpaPredicateExecutorImpl(QuerydslJpaPredicateExecutor querydslPredicateExecutor) {
        this.querydslPredicateExecutor = querydslPredicateExecutor;
    }

    @Override
    public Optional<T> findOne(Predicate predicate) {
        return querydslPredicateExecutor.findOne(predicate);
    }

    @Override
    public Iterable<T> findAll(Predicate predicate) {
        return querydslPredicateExecutor.findAll(predicate);
    }

    @Override
    public Iterable<T> findAll(Predicate predicate, Sort sort) {
        return querydslPredicateExecutor.findAll(predicate, sort);
    }

    @Override
    public Iterable<T> findAll(Predicate predicate, OrderSpecifier... orders) {
        return querydslPredicateExecutor.findAll(predicate, orders);
    }

    @Override
    public Iterable<T> findAll(OrderSpecifier... orders) {
        return querydslPredicateExecutor.findAll(orders);
    }

    @Override
    public Page<T> findAll(Predicate predicate, Pageable pageable) {
        return querydslPredicateExecutor.findAll(predicate, pageable);
    }

    @Override
    public long count(Predicate predicate) {
        return querydslPredicateExecutor.count(predicate);
    }

    @Override
    public boolean exists(Predicate predicate) {
        return querydslPredicateExecutor.exists(predicate);
    }

    @Override
    public String ciaoCiao() {
        
        /*
        Assert.notNull(predicate, "Predicate must not be null!");
		Assert.notNull(pageable, "Pageable must not be null!");

		final JPQLQuery<?> countQuery = createCountQuery(predicate);
		JPQLQuery<T> query = querydsl.applyPagination(pageable, createQuery(predicate).select(path));

		return PageableExecutionUtils.getPage(query.fetch(), pageable, countQuery::fetchCount);
        */
        return "ciao minchia";
    }

    
}
