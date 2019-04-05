package it.nextsw.common.interceptors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

public class ParameterizedInterceptor {

    InterceptorParameters parameters;

    private NextSdrControllerInterceptor.InterceptorOperation operation;
    private NextSdrControllerInterceptor.InterceptorType type;
    // Lista che indica tutto il "percorso" da applicare all'entità principale di lavoro per arrivare alla entità discendente
    // a cui si riferisce l'interceptor; può contenere o oggetti Method che possono essere invocati direttamente, oppure degli interi
    // che indicano l'indice da applicare alla collection ottenuta dall'invocazione del metodo presente subito prima
    private ArrayList<Object> getMethodsPaths;

    public ParameterizedInterceptor(NextSdrControllerInterceptor.InterceptorOperation operation, NextSdrControllerInterceptor.InterceptorType type, InterceptorParameters params){
        this.setOperation(operation);
        this.setType(type);
        this.parameters = params;
    }

    public ParameterizedInterceptor(NextSdrControllerInterceptor.InterceptorOperation operation, NextSdrControllerInterceptor.InterceptorType type,  ArrayList<Object> getMethodsPaths, Object entity, Object beforeUpdateEntity, HttpServletRequest request, Map<String, String> additionalData, boolean mainEntity, Class projection){
        this.setOperation(operation);
        this.setType(type);
        this.setGetMethodsPaths(getMethodsPaths);
        this.parameters = new InterceptorParameters(entity, beforeUpdateEntity, additionalData, request, mainEntity, projection);
    }

    public ParameterizedInterceptor(NextSdrControllerInterceptor.InterceptorOperation operation, NextSdrControllerInterceptor.InterceptorType type, ArrayList<Object> getMethodsPaths, Object entity,  HttpServletRequest request, Map<String, String> additionalData, boolean mainEntity, Class projection){
        this.setOperation(operation);
        this.setType(type);
        this.setGetMethodsPaths(getMethodsPaths);
        this.parameters = new InterceptorParameters(entity, additionalData, request, mainEntity, projection);
    }

    public InterceptorParameters getParameters() {
        return parameters;
    }

    public void setParameters(InterceptorParameters parameters) {
        this.parameters = parameters;
    }

    public NextSdrControllerInterceptor.InterceptorOperation getOperation() {
        return operation;
    }

    public void setOperation(NextSdrControllerInterceptor.InterceptorOperation operation) {
        this.operation = operation;
    }

    public NextSdrControllerInterceptor.InterceptorType getType() {
        return type;
    }

    public void setType(NextSdrControllerInterceptor.InterceptorType type) {
        this.type = type;
    }

    public ArrayList<Object> getGetMethodsPaths() {
        return getMethodsPaths;
    }

    public void setGetMethodsPaths(ArrayList<Object> getMethodsPaths) {
        // In questo caso faccio una copia della collection per evitare il passaggio per referenza: non voglio che vengano
        // riflesse eventuali modifiche apportate alla lista ma voglio tenerla così come passata in questo momento.
        this.getMethodsPaths = new ArrayList<Object>();
        this.getMethodsPaths.addAll(getMethodsPaths);
    }
}
