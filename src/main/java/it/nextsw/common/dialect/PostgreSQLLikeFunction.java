package it.nextsw.common.dialect;

import it.nextsw.common.repositories.StringOperation;
import java.util.List;
import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 *
 * @author gusgus
 */
public class PostgreSQLLikeFunction implements SQLFunction {

    public PostgreSQLLikeFunction() {
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
        String value = (String) arguments.get(1);
        String operation = (String)arguments.get(2);
        String likeOperator = "like";
        
        StringOperation.Operators operator = StringOperation.Operators.valueOf(operation);
        
        switch (operator) {
            case contains:
                value = "%" + value + "%"; 
                break;
            case containsIgnoreCase:
                value = "%" + value + "%"; 
                likeOperator = "ilike";
                break;
            case startsWith:
                value = value + "%"; 
                break;
            case startsWithIgnoreCase:
                value = value + "%"; 
                 likeOperator = "ilike";
                break;
            case equalsIgnoreCase:
                 likeOperator = "ilike";
                break;
            default:
                throw new QueryException(String.format("operatore %s non valido", operator));
        }
        
        String stringPredicate = field + " " + likeOperator + " '" + value + "'";
        return stringPredicate;
    }
}
