package it.nextsw.common.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.nextsw.common.utils.ApplicationContextUtil;

/**
 *
 * @author gdm
 * 
 * Questa è l'oggetto in cui viene convertito il json passato nella query string quando si vuole effettuare un filtro su un campo json
 * 
 */
public class AbstractJsonTypeForQueryDslExecutor extends AbstractJsonType {
    public JsonNode jsonNode;

    public AbstractJsonTypeForQueryDslExecutor() {
    }
    
    public AbstractJsonTypeForQueryDslExecutor(JsonNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    public JsonNode getJsonNode() {
        return jsonNode;
    }

    public void setJsonNode(JsonNode jsonNode) {
        this.jsonNode = jsonNode;
    }
    
    public String toJsonString() throws JsonProcessingException {
        ObjectMapper objectMapper = ApplicationContextUtil.getApplicationContext().getBean(ObjectMapper.class);
        return objectMapper.writeValueAsString(jsonNode);
    }
}
