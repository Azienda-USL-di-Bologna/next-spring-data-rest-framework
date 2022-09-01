package it.nextsw.common.repositories;

import java.io.Serializable;
import javax.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.QuerydslJpaPredicateExecutor;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryFragment;

/**
 * Custom JpaRepositoryFactory allowing to support a custom QuerydslJpaRepository.
 *
 */
public class CustomRepositoryFactory extends JpaRepositoryFactory {

    private final EntityManager entityManager;

    public CustomRepositoryFactory(EntityManager entityManager) {
      super(entityManager);
      this.entityManager = entityManager;
    }


    @Override
    protected RepositoryComposition.RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {
        final RepositoryComposition.RepositoryFragments[] modifiedFragments = {RepositoryComposition.RepositoryFragments.empty()};
        RepositoryComposition.RepositoryFragments fragments = super.getRepositoryFragments(metadata);
        // because QuerydslJpaPredicateExecutor is using som internal classes only a wrapper can be used.
        JpaEntityInformation<?, Serializable> entityInformation =  getEntityInformation(metadata.getDomainType());
        fragments.stream().forEach(
                f -> {
                    if (f.getImplementation().isPresent() &&
                            QuerydslJpaPredicateExecutor.class.isAssignableFrom(f.getImplementation().get().getClass())) {
                        modifiedFragments[0] = modifiedFragments[0].append(RepositoryFragment.implemented(
//                                new NextQuerydslJpaPredicateExecutorImpl2((QuerydslJpaPredicateExecutor) f.getImplementation().get())
                                new NextQuerydslJpaPredicateExecutorNoCountImpl(entityInformation, entityManager)
                                ));
                    } else {
                        modifiedFragments[0].append(f);
                    }
                }
        );
        return modifiedFragments[0];
    }
    
//    @Override
//  protected RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {
//    RepositoryFragments fragments = super.getRepositoryFragments(metadata);
//
//    if (NextSdrQueryDslRepository.class.isAssignableFrom(
//        metadata.getRepositoryInterface())) {
//
//      JpaEntityInformation<?, Serializable> entityInformation = 
//          getEntityInformation(metadata.getDomainType());
//
//      Object queryableFragment = getTargetRepositoryViaReflection(
//          NextQuerydslJpaPredicateExecutorImpl.class, entityInformation, entityManager);
//
//      fragments = fragments.append(RepositoryFragment.implemented(queryableFragment));
//    }
//
//    return fragments;
//  }
}