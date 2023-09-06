package it.nextsw.common.dialect;

import java.util.List;
import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
public class PostgresArrayFunctions implements SQLFunction {

    public PostgresArrayFunctions() {
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
        String array = (String) arguments.get(0);
        array = StringUtils.trimTrailingCharacter(StringUtils.trimLeadingCharacter(StringUtils.trimTrailingCharacter(array, '\''), '\''), ',');
        String type = (String) arguments.get(1);
        type = StringUtils.trimLeadingCharacter(StringUtils.trimTrailingCharacter(type, '\''), '\'');
        String field = (String) arguments.get(2);
        String operation = (String) arguments.get(3);
        operation = StringUtils.trimLeadingCharacter(StringUtils.trimTrailingCharacter(operation, '\''), '\'');
        String condition = String.format("string_to_array('%s', ',')::%s %s %s", array, type, operation, field);
        return condition;
    }
    
}
