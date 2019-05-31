package it.nextsw.common.interceptors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Map;

public class InterceptorParameters {

    Object entity;

    Object beforeUpdateEntity;

    Map<String, String> additionalData;

    HttpServletRequest request;

    boolean mainEntity;

    Class projection;


    public InterceptorParameters(Object entity, Object beforeUpdateEntity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projection){
        this.setEntity(entity);
        this.setBeforeUpdateEntity(beforeUpdateEntity);
        this.setAdditionalData(additionalData);
        this.setRequest(request);
        this.setMainEntity(mainEntity);
        this.setProjection(projection);
    }

    public InterceptorParameters(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projection){
        this.setEntity(entity);
        this.setAdditionalData(additionalData);
        this.setRequest(request);
        this.setMainEntity(mainEntity);
        this.setProjection(projection);
    }

    public Object getEntity() {
        return entity;
    }

    public void setEntity(Object entity) {
        this.entity = entity;
    }

    public Object getBeforeUpdateEntity() {
        return beforeUpdateEntity;
    }

    public void setBeforeUpdateEntity(Object beforeUpdateEntity) {
        this.beforeUpdateEntity = beforeUpdateEntity;
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
