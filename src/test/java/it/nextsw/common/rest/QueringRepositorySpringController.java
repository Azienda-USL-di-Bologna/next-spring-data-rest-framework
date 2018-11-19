package it.nextsw.common.rest;

import com.querydsl.core.types.Predicate;
import it.nextsw.common.controller.RestControllerEngine;
import it.nextsw.common.controller.exceptions.RestControllerEngineException;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import it.nextsw.common.persistence.entities.Agente;
import it.nextsw.common.persistence.entities.QAgente;
import it.nextsw.common.utils.exceptions.EntityReflectionException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = "${configurazione.mapping.url.root}")
public class QueringRepositorySpringController extends RestControllerEngine {
    private static final Logger logger = Logger.getLogger(QueringRepositorySpringController.class);

    @Autowired
    private RestControllerEngineImpl restControllerEngine;

    @RequestMapping(value = {"/agente", "/agente/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> agenteQuering(
            @QuerydslPredicate(root = Agente.class) Predicate predicate,
            Pageable pageable,
            @RequestParam(required = false) String projection,
            @PathVariable(required = false) Integer id,
            HttpServletRequest request,
            @RequestParam(required = false, name = "additionalData") String additionalData) throws ClassNotFoundException, EntityReflectionException, IllegalArgumentException, IllegalAccessException, RestControllerEngineException, AbortLoadInterceptorException {

        try {
            Object resource = restControllerEngine.getResources(request, id, projection, predicate, pageable, additionalData, QAgente.agente, Agente.class);
            return ResponseEntity.ok(resource);
        } catch (Exception e) {
            logger.error("", e);
            throw e;
        }

    }

}
