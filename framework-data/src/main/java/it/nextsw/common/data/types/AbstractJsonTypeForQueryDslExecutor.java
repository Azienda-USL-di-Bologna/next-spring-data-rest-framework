package it.nextsw.common.data.types;

import it.nextsw.common.data.types.AbstractJsonType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.nextsw.common.data.utils.NextSDRDataApplicationContextUtil;

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
        ObjectMapper objectMapper = NextSDRDataApplicationContextUtil.getApplicationContext().getBean(ObjectMapper.class);
        return objectMapper.writeValueAsString(jsonNode);
    }
}
