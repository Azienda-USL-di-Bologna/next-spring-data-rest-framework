package it.nextsw.common.rest;


import it.nextsw.common.controller.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "${configurazione.mapping.url.root}")
public class CrudController extends BaseCrudController {

}
