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
public class PostgreSQLJsonbFunction implements SQLFunction {

    public PostgreSQLJsonbFunction() {
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
        String field = (String) arguments.get(0);
        //String jsonPathField = (String) arguments.get(1);
        String value = (String) arguments.get(1);
        String jsonbContains = field + " @> " + value + "::jsonb";
//        System.out.println("jsonbContains " + jsonbContains);
        return jsonbContains;
    }
}
