package it.nextsw.common.controller;

import java.util.Map;

/**
 *
 * @author gdm
 */
public class BatchOperation {

    public static enum Operations {INSERT, UPDATE, DELETE}
    private Operations operation;
    private Object id;
    private String entityPath;
    private String returnProjection;
    private Object entityBody;
    private Map<String, String> additionalData;

    public BatchOperation() {
    }

    public BatchOperation(Operations operation, Object id, String entityPath, String returnProjection, Map<String, Object> entityBody, Map<String, String> additionalData) {
        this.operation = operation;
        this.id = id;
        this.entityPath = entityPath;
        this.returnProjection = returnProjection;
        this.entityBody = entityBody;
        this.additionalData = additionalData;
    }

    public Operations getOperation() {
        return operation;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public void setOperation(Operations operation) {
        this.operation = operation;
    }

    public String getEntityPath() {
        if (!entityPath.startsWith("/"))
            entityPath = "/" + entityPath;
        return entityPath;
    }

    public void setEntityPath(String entityPath) {
        this.entityPath = entityPath;
    }

    public String getReturnProjection() {
        return returnProjection;
    }

    public void setReturnProjection(String returnProjection) {
        this.returnProjection = returnProjection;
    }

    public Object getEntityBody() {
        return entityBody;
    }

    public void setEntityBody(Object entityBody) {
        this.entityBody = entityBody;
    }

    public Map<String, String> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Map<String, String> additionalData) {
        this.additionalData = additionalData;
    }
}
