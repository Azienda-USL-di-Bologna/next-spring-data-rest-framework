package it.nextsw.common.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.nextsw.common.controller.exceptions.NotFoundResourceException;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.exceptions.AbortSaveInterceptorException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author gdm
 */
public abstract class BaseCrudController extends RestControllerEngine {

    private final Logger log = LoggerFactory.getLogger(RestControllerEngine.class);

    @RequestMapping(value = {"*"}, method = {RequestMethod.POST, RequestMethod.PUT})
    @Transactional(rollbackFor = {Throwable.class})
    public ResponseEntity<?> insertResource(
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, AbortSaveInterceptorException {
        log.info("executing insert operation...");
        Object entity = super.insert(data, request, parseAdditionalDataIntoMap(additionalData), null, false);
        return new ResponseEntity(entity, HttpStatus.CREATED);
    }

    @RequestMapping(value = {"*/{id}"}, method = RequestMethod.PATCH)
    @Transactional(rollbackFor = {Throwable.class})
    public ResponseEntity<?> updateResource(
            @PathVariable(required = true) Object id,
            @RequestBody Map<String, Object> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException {
        log.info("executing update operation...");
        try {
            Object update = super.update(id, data, request, parseAdditionalDataIntoMap(additionalData), null, false);
            return new ResponseEntity(update, HttpStatus.OK);
        } catch (NotFoundResourceException ex) {
            return new ResponseEntity(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = {"*/{id}"}, method = RequestMethod.DELETE)
    @Transactional(rollbackFor = {Throwable.class})
    public ResponseEntity<?> deleteResource(
            @PathVariable(required = true) Object id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, AbortSaveInterceptorException {

        log.info("executing delete operation...");

        try {
            super.delete(id, request, parseAdditionalDataIntoMap(additionalData), null, false);
            if (true)
            throw new AbortSaveInterceptorException("aaaa");
            return new ResponseEntity(HttpStatus.OK);
        } catch (NotFoundResourceException ex) {
            return new ResponseEntity(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = {"batch"}, method = RequestMethod.POST)
    @Transactional(rollbackFor = {Throwable.class})
    public void batchResources(
            @RequestBody List<BatchOperation> data,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws RestControllerEngineException, AbortSaveInterceptorException, JsonProcessingException, NotFoundResourceException, NullPointerException {
        try {
            log.info("executing batch operation...");
            batch(data, request, additionalData);
//            return new ResponseEntity(HttpStatus.OK);
        }
        catch (JsonProcessingException | NotFoundResourceException | NullPointerException ex) {
            throw ex;
        }
    }
}
