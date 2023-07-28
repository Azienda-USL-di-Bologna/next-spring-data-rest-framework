package it.nextsw.common.repositories;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.lang.reflect.Field;

/**
 *
 * @author gusgus & gdm & chatgpt (soprattutto gdm)
 * 
 * Classe che rappresenta in tipo json. Da estendere in tutte le classi delle entità che rappresentano una colonna di tipo jsonb
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS, visible = true, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "classz", defaultImpl = AbstractJsonTypeForQueryDslExecutor.class)
public abstract class AbstractJsonType {
    
    public String classz;

    public String getClassz() {
        return classz;
    }

    public void setClassz(String classz) {
        this.classz = classz;
    }
    
    // Metodo equals generico per confrontare due oggetti JSON
    @Override
    public boolean equals(Object other) {
        try {
            Field[] declaredFields = this.getClass().getDeclaredFields();
            for (Field declaredField : declaredFields) {
                declaredField.setAccessible(true);
                Object valueThis = declaredField.get(this);
                Object valueOther = declaredField.get(other);
                
                if (valueThis == null && valueOther != null || valueThis != null && valueOther == null) {
                    return false;
                }
                
                if (valueThis != null && valueOther != null) {
                    if (!valueThis.equals(valueOther)) {
                        return false;
                    }
                }
            }
            
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
