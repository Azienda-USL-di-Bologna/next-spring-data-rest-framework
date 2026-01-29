package it.nextsw.common.dialect;

import it.nextsw.common.repositories.StringOperation;
import static it.nextsw.common.repositories.StringOperation.Operators.contains;
import static it.nextsw.common.repositories.StringOperation.Operators.containsIgnoreCase;
import static it.nextsw.common.repositories.StringOperation.Operators.equalsIgnoreCase;
import static it.nextsw.common.repositories.StringOperation.Operators.startsWith;
import static it.nextsw.common.repositories.StringOperation.Operators.startsWithIgnoreCase;
import java.util.List;
import org.hibernate.QueryException;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 *
 * @author gusgus
 */
public class PostgreSQLLikeFunction extends StandardSQLFunction {

    public PostgreSQLLikeFunction(String name) {
        super(name, true, StandardBasicTypes.BOOLEAN);
    }

//    public PostgreSQLLikeFunction() {
//    }
//
//    @Override
//    public boolean hasArguments() {
//        return true;
//    }
//
//    @Override
//    public boolean hasParenthesesIfNoArguments() {
//        return true;
//    }
//
//    @Override
//    public Type getReturnType(Type firstArgumentType, Mapping mapping) throws QueryException {
//        return StandardBasicTypes.BOOLEAN;
//    }
//    @Override
//    public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) throws QueryException {
//        String field = (String) arguments.get(0);
//        String value = (String) arguments.get(1);
//        String operation = (String)arguments.get(2);
//        String likeOperator = "like";
//
//        StringOperation.Operators operator = StringOperation.Operators.valueOf(operation);
//
//        // tolgo gli apici all'inizio e alla fine della stringa
//        value = value.substring(1, value.length() - 1);
//
//        switch (operator) {
//            case contains:
//                value = "%" + value + "%";
//                break;
//            case containsIgnoreCase:
//                value = "%" + value + "%";
//                likeOperator = "ilike";
//                break;
//            case startsWith:
//                value = value + "%";
//                break;
//            case startsWithIgnoreCase:
//                value = value + "%";
//                 likeOperator = "ilike";
//                break;
//            case equalsIgnoreCase:
//                 likeOperator = "ilike";
//                break;
//            default:
//                throw new QueryException(String.format("operatore %s non valido", operator));
//        }
//
//        String stringPredicate = field + " " + likeOperator + " '" + value + "'";
//        return stringPredicate;
//    }
    @Override
    public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, ReturnableType<?> returnType, SqlAstTranslator<?> translator) {
        SqlAstNode fieldArgument = sqlAstArguments.get(0);
        SqlAstNode valueArgument = sqlAstArguments.get(1);
        SqlAstNode operationArgument = sqlAstArguments.get(2);

        Literal valueLiteral = (Literal) valueArgument;
        String value = (String) valueLiteral.getLiteralValue();
        value = value.replace("'", "''");

        Literal operationLiteral = (Literal) operationArgument;
        String operation = (String) operationLiteral.getLiteralValue();

        String likeOperator = "like";

        StringOperation.Operators operator = StringOperation.Operators.valueOf(operation);

        switch (operator) {
            case contains ->
                value = "%" + value + "%";
            case containsIgnoreCase -> {
                value = "%" + value + "%";
                likeOperator = "ilike";
            }
            case startsWith ->
                value = value + "%";
            case startsWithIgnoreCase -> {
                value = value + "%";
                likeOperator = "ilike";
            }
            case equalsIgnoreCase ->
                likeOperator = "ilike";
            case notEqualsIgnoreCase ->
                likeOperator = "not ilike";

            default ->
                throw new QueryException(String.format("operatore %s non valido", operator), "");
        }

        fieldArgument.accept(translator);
        sqlAppender.append(" ");
        sqlAppender.append(likeOperator);
        sqlAppender.append(" '");
        sqlAppender.append(value);
        sqlAppender.append("'");
    }
}
