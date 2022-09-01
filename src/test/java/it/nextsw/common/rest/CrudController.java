package it.nextsw.common.rest;


import it.nextsw.common.controller.BaseCrudController;
import it.nextsw.common.controller.RestControllerEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

@RestController
@RequestMapping(value = "${configurazione.mapping.url.root}")
public class CrudController extends BaseCrudController {

    @Autowired
    private RestControllerEngineImpl restControllerEngine;

    @Override
    public RestControllerEngine getRestControllerEngine() {
        return restControllerEngine;
    }
}
