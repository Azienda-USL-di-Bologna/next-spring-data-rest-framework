package it.nextsw.common.dialect;

import java.util.List;
import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 *
 * @author gdm
 */
public class PostgreSQLFullTextSearchFunction implements SQLFunction {

    public PostgreSQLFullTextSearchFunction() {
    }

    @Override
    public boolean hasArguments() {
        return true;
    }

    @Override
    public boolean hasParenthesesIfNoArguments() {
        return true;
    }

    @Override
    public Type getReturnType(Type firstArgumentType, Mapping mapping) throws QueryException {
        return StandardBasicTypes.BOOLEAN;
    }

    @Override
    public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) throws QueryException {
        String ftsConfig = (String) arguments.get(0);
        String field = (String) arguments.get(1);
        String value = "";
        if (arguments.get(2) != null) {
            value = (String) arguments.get(2);
            // tolgo gli apici all'inizio e alla fine della stringa
            value = value.substring(1, value.length() - 1);
            // al posto degli spazi metto le & per fare la ricerca in and e :* per trovare anche le parole che hanno la stringa come radice
            value = value.trim().replaceAll("\\s+", ":*&");
            
            // faccio l'escape dei caratteri speciali
            value = value
                    .replace("\\", "\\\\")
                    .replace("!", "\\!")
                    .replace("|", "\\|")
                    .replace("'", "\\''")
                    .replace(":", "\\:")
                    .replace("&", "\\&")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace("+", "\\+")
                    .replace("<", "\\<");
        }
        if (value.trim().isEmpty()) {
            value = "\\";
        }
        String tsCondition = " to_tsquery ('" + ftsConfig + "',$$" + value + ":*$$) @@ " + field;
        return tsCondition;
    }
    
}
