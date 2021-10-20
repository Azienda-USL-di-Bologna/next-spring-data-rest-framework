package it.nextsw.common.controller;

import it.nextsw.common.utils.EntityReflectionUtils;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import javax.persistence.EntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author gdm
 */
public class BeforeUpdateEntityApplier {
    private final Object currentEntity;
    private final EntityManager entityManager;

    public BeforeUpdateEntityApplier(Object currentEntity, EntityManager entityManager) {
       this.currentEntity = currentEntity;
       this.entityManager = entityManager;
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void beforeUpdateApply(Consumer<Object> fn) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
//        Object currentEntityObj = this.currentEntity.get();
        Object entityBeforeModify = entityManager.find(currentEntity.getClass(), EntityReflectionUtils.getPrimaryKeyValue(currentEntity));
        fn.accept(entityBeforeModify);
    }
}
