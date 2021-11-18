package it.nextsw.common.repositories;

import com.querydsl.core.SimpleQuery;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPQLQuery;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.jpa.repository.support.QuerydslJpaPredicateExecutor;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
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
public class NextQuerydslJpaPredicateExecutorNoCountImpl<E extends Object, ID extends Object, T extends EntityPath<?>> extends QuerydslJpaPredicateExecutor
        implements NextSdrQueryDslRepository {

    private final JpaEntityInformation<T, ?> entityInformation;
	private final EntityPath<T> path;
	private final Querydsl querydsl;
	private final EntityManager entityManager;
//	private final CrudMethodMetadata metadata;
        private static final EntityPathResolver DEFAULT_ENTITY_PATH_RESOLVER = SimpleEntityPathResolver.INSTANCE;
        
    public NextQuerydslJpaPredicateExecutorNoCountImpl(JpaEntityInformation entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager, DEFAULT_ENTITY_PATH_RESOLVER, null);
        path = DEFAULT_ENTITY_PATH_RESOLVER.createPath(entityInformation.getJavaType());
        this.entityInformation = entityInformation;
		this.querydsl = new Querydsl(entityManager, new PathBuilder<T>(path.getType(), path.getMetadata()));
		this.entityManager = entityManager;
    }


    @Override
    public Optional<T> findOne(Predicate predicate) {
        return super.findOne(predicate);
    }

    @Override
    public List<T> findAll(Predicate predicate) {
        return super.findAll(predicate);
    }

    @Override
    public List<T> findAll(Predicate predicate, Sort sort) {
        return super.findAll(predicate, sort);
    }

    @Override
    public List<T> findAll(Predicate predicate, OrderSpecifier... orders) {
        return super.findAll(predicate, orders);
    }

    @Override
    public List<T> findAll(OrderSpecifier... orders) {
        return super.findAll(orders);
    }

    @Override
    public Page<T> findAll(Predicate predicate, Pageable pageable) {
        System.out.println("in findall");
        return super.findAll(predicate, pageable);
//        return findAllNoCount(predicate, pageable);
    }
    
    @Override
    public Page<T> findAllNoCount(Predicate predicate, Pageable pageable) {
        System.out.println("in findall NO COUNT");
        int oneMore = pageable.getPageSize() + 1;
        JPQLQuery query = (JPQLQuery) createQuery(predicate)
                .offset(pageable.getOffset())
                .limit(oneMore);

        Sort sort = pageable.getSort();
        query = querydsl.applySorting(sort, query);

        List<T> entities = query.fetch();

        int size = entities.size();
        if (size > pageable.getPageSize())
            entities.remove(size - 1);

        return new PageImpl<>(entities, pageable, pageable.getOffset() + size);
    }

    @Override
    public long count(Predicate predicate) {
        return super.count(predicate);
    }

    @Override
    public boolean exists(Predicate predicate) {
        return super.exists(predicate);
    }
}
