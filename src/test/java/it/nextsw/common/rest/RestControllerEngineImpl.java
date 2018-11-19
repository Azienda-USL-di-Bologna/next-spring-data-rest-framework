package it.nextsw.common.rest;

import it.nextsw.common.controller.RestControllerEngine;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;


@Service
public class RestControllerEngineImpl extends RestControllerEngine {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    protected Object retriveEntity(Class entityClass, Object entityKey) {
        return entityManager.find(entityClass, ((Integer)entityKey).longValue());
    }
}
