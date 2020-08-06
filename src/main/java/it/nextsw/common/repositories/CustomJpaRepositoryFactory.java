package it.nextsw.common.repositories;

import javax.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.QuerydslJpaPredicateExecutor;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;

/**
 * Custom JpaRepositoryFactory allowing to support a custom QuerydslJpaRepository.
 *
 */
public class CustomJpaRepositoryFactory extends JpaRepositoryFactory {

    /**
     * Creates a new {@link JpaRepositoryFactory}.
     *
     * @param entityManager must not be {@literal null}
     */
    public CustomJpaRepositoryFactory(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    protected RepositoryComposition.RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {
        final RepositoryComposition.RepositoryFragments[] modifiedFragments = {RepositoryComposition.RepositoryFragments.empty()};
        RepositoryComposition.RepositoryFragments fragments = super.getRepositoryFragments(metadata);
        // because QuerydslJpaPredicateExecutor is using som internal classes only a wrapper can be used.
        fragments.stream().forEach(
                f -> {
                    if (f.getImplementation().isPresent() &&
                            QuerydslJpaPredicateExecutor.class.isAssignableFrom(f.getImplementation().get().getClass())) {
                        modifiedFragments[0] = modifiedFragments[0].append(RepositoryFragment.implemented(
                                new NextQuerydslJpaPredicateExecutorImpl((QuerydslJpaPredicateExecutor) f.getImplementation().get())));
                    } else {
                        modifiedFragments[0].append(f);
                    }
                }
        );
        return modifiedFragments[0];
    }
}