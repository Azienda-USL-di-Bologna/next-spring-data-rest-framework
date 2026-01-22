package it.nextsw.common.dialect;

import java.util.List;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;

/**
 *
 * @author gdm
 */
public class PostgreSQLJsonbFunction extends StandardSQLFunction {

    @Override
    public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, ReturnableType<?> returnType, SqlAstTranslator<?> translator) {
        sqlAstArguments.get(0).accept(translator);
        sqlAppender.append(" @> ");
        sqlAstArguments.get(1).accept(translator);
        sqlAppender.append("::jsonb");
    }

    public PostgreSQLJsonbFunction(String name) {
        super(name, true, StandardBasicTypes.BOOLEAN);
    }

//    public PostgreSQLJsonbFunction() {
//    }

//    @Override
//    public boolean hasArguments() {
//        return true;
//    }

//    @Override
//    public boolean hasParenthesesIfNoArguments() {
//        return true;
//    }

//    @Override
//    public Type getReturnType(Type firstArgumentType, Mapping mapping) throws QueryException {
//        return StandardBasicTypes.BOOLEAN;
//    }

//    @Override
//    public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> arguments, SqlAstTranslator<?> translator) {
////    public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) throws QueryException {
//        String field = (String) arguments.get(0);
//        //String jsonPathField = (String) arguments.get(1);
//        String value = (String) arguments.get(1);
//        String jsonbContains = field + " @> " + value + "::jsonb";
////        System.out.println("jsonbContains " + jsonbContains);
//        return jsonbContains;
//    }
}
