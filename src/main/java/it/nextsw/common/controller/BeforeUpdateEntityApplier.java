package it.nextsw.common.controller;

import it.nextsw.common.controller.exceptions.BeforeUpdateEntityApplierException;
import it.nextsw.common.utils.EntityReflectionUtils;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author gdm
 */
@Component
public class BeforeUpdateEntityApplier {

    private final ThreadLocal<Object> currentEntity = new ThreadLocal<>();

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void beforeUpdateApply(Consumer<Object> fn) throws BeforeUpdateEntityApplierException {
        Object entityBeforeModify;
        try {
            entityBeforeModify = entityManager.find(EntityReflectionUtils.getEntityFromProxyObject(currentEntity.get()), EntityReflectionUtils.getPrimaryKeyValue(currentEntity.get()));
            fn.accept(entityBeforeModify);
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new BeforeUpdateEntityApplierException("errore nel reperire l'entità prima delle modifiche dal database", ex);
        } catch (Exception ex) {
            throw new BeforeUpdateEntityApplierException("errore nel BeforeUpdateEntityApplier", ex);
        }

    }

    public void setCurrentEntity(Object currentEntity) {
        this.currentEntity.set(currentEntity);
    }
}
