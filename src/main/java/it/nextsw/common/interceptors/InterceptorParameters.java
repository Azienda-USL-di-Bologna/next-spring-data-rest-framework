package it.nextsw.common.interceptors;

import it.nextsw.common.controller.BeforeUpdateEntityApplier;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class InterceptorParameters {

    private Object entity;

    private BeforeUpdateEntityApplier beforeUpdateEntityApplier;

    private Map<String, String> additionalData;

    private HttpServletRequest request;

    private boolean mainEntity;

    private Class projection;

    public InterceptorParameters(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projection){
        this.entity = entity;
        this.additionalData = additionalData;
        this.request = request;
        this.mainEntity = mainEntity;
        this.projection = projection;
    }

    public InterceptorParameters(Object entity, BeforeUpdateEntityApplier beforeUpdateEntityApplier, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projection){
        this(entity, additionalData, request, mainEntity, projection);
        this.beforeUpdateEntityApplier = beforeUpdateEntityApplier;
    }

    public Object getEntity() {
        return entity;
    }

    public void setEntity(Object entity) {
        this.entity = entity;
    }

    public BeforeUpdateEntityApplier getBeforeUpdateEntity() {
        return beforeUpdateEntityApplier;
    }

    public void setBeforeUpdateEntity(BeforeUpdateEntityApplier beforeUpdateEntityApplier) {
        this.beforeUpdateEntityApplier = beforeUpdateEntityApplier;
    }

    public Map<String, String> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Map<String, String> additionalData) {
        this.additionalData = additionalData;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public boolean isMainEntity() {
        return mainEntity;
    }

    public void setMainEntity(boolean mainEntity) {
        this.mainEntity = mainEntity;
    }

    public Class getProjection() {
        return projection;
    }

    public void setProjection(Class projection) {
        this.projection = projection;
    }
}
