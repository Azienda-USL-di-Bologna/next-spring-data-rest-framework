package it.nextsw.common.dialect;

import java.util.List;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
public class PostgresArrayFunctions extends StandardSQLFunction {

    public PostgresArrayFunctions(String name) {
        super(name, true, StandardBasicTypes.BOOLEAN);
    }

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

    @Override
    public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, ReturnableType<?> returnType, SqlAstTranslator<?> translator) {
        SqlAstNode arrayArgument = sqlAstArguments.get(0);
        SqlAstNode typeArgument = sqlAstArguments.get(1);
        SqlAstNode fieldArgument = sqlAstArguments.get(2);
        SqlAstNode operationArgument = sqlAstArguments.get(3);
        
        Literal arrayLiteral = (Literal) arrayArgument;
        String array = (String) arrayLiteral.getLiteralValue();
        array = StringUtils.trimTrailingCharacter(StringUtils.trimLeadingCharacter(StringUtils.trimTrailingCharacter(array, '\''), '\''), ',');
        
        Literal typeLiteral = (Literal) typeArgument;
        String type = (String) typeLiteral.getLiteralValue();
        type = StringUtils.trimLeadingCharacter(StringUtils.trimTrailingCharacter(type, '\''), '\'');
        
        Literal operationLiteral = (Literal) operationArgument;
        String operation = (String) operationLiteral.getLiteralValue();
        operation = StringUtils.trimLeadingCharacter(StringUtils.trimTrailingCharacter(operation, '\''), '\'');
        
        sqlAppender.append("string_to_array('");
        sqlAppender.append(array);
        sqlAppender.append("', ',')::");
        sqlAppender.append(type);
        sqlAppender.append(" ");
        sqlAppender.append(operation);
        sqlAppender.append(" ");
        fieldArgument.accept(translator);
    }
    
//    @Override
//    public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) throws QueryException {
//        String array = (String) arguments.get(0);
//        array = StringUtils.trimTrailingCharacter(StringUtils.trimLeadingCharacter(StringUtils.trimTrailingCharacter(array, '\''), '\''), ',');
//        String type = (String) arguments.get(1);
//        type = StringUtils.trimLeadingCharacter(StringUtils.trimTrailingCharacter(type, '\''), '\'');
//        String field = (String) arguments.get(2);
//        String operation = (String) arguments.get(3);
//        operation = StringUtils.trimLeadingCharacter(StringUtils.trimTrailingCharacter(operation, '\''), '\'');
//        
//        String condition = String.format("string_to_array('%s', ',')::%s %s %s", array, type, operation, field);
//        return condition;
//
//    }
    
}
