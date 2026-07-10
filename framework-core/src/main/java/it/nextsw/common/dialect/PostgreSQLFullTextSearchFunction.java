package it.nextsw.common.dialect;

import java.util.List;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.sql.ast.tree.expression.Literal;

/**
 *
 * @author gdm
 */
public class PostgreSQLFullTextSearchFunction extends StandardSQLFunction {

    public PostgreSQLFullTextSearchFunction(String name) {
        super(name, true, StandardBasicTypes.BOOLEAN);
    }

//    public void render(
//                SqlAppender sqlAppender,
//                List<? extends SqlAstNode> arguments,
//                SqlAstTranslator<?> walker) {
//    }
     @Override
    public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, ReturnableType<?> returnType, SqlAstTranslator<?> translator) {
 
        SqlAstNode ftsConfigArgument = sqlAstArguments.get(0);
        SqlAstNode fieldArgument = sqlAstArguments.get(1);
        SqlAstNode valueArgument = sqlAstArguments.size() > 2 ? sqlAstArguments.get(2): null;
        
        String value = "";
        if (valueArgument != null) {
            Literal valueLiteral = (Literal) valueArgument;
            value = (String) valueLiteral.getLiteralValue();
            if (value != null) {
               // al posto di alcuni caratteri speciali, inserisco uno spazio
                value = value
                    .replace("@", " ")
                    .replace(".", " ");

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

                // al posto degli spazi metto le & per fare la ricerca in and e :* per trovare anche le parole che hanno la stringa come radice
                //value = value.trim().replaceAll("\\s+", ":*&");
                // Sostituita con questa per BBK-2788, in modo che le lettere singole non ricevano il ":*" altrimenti trovano tutto
                value = value.trim().replaceAll("\\S{2,}\\s", ":*&");
                value = value.trim().replaceAll("\\s+", "&");
            } else {
                value = "";
            }
        }
        if (value.trim().isEmpty()) {
            value = "\\";
        }
        sqlAppender.append(" to_tsquery (");
        ftsConfigArgument.accept(translator);
        sqlAppender.append(",$$");
        sqlAppender.append(value);
        sqlAppender.append(":*$$) @@ ");
        fieldArgument.accept(translator);

    }
    
//    @Override
//    public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) throws QueryException {
//        String ftsConfig = (String) arguments.get(0);
//        String field = (String) arguments.get(1);
//        String value = "";
//        if (arguments.get(2) != null) {
//            value = (String) arguments.get(2);
//            // tolgo gli apici all'inizio e alla fine della stringa
//            value = value.substring(1, value.length() - 1);
//            
//            // al posto di alcuni caratteri speciali, inserisco uno spazio
//            value = value
//                    .replace("@", " ")
//                    .replace(".", " ");
//            
//            // faccio l'escape dei caratteri speciali
//            value = value
//                    .replace("\\", "\\\\")
//                    .replace("!", "\\!")
//                    .replace("|", "\\|")
//                    .replace("'", "\\''")
//                    .replace(":", "\\:")
//                    .replace("&", "\\&")
//                    .replace("(", "\\(")
//                    .replace(")", "\\)")
//                    .replace("+", "\\+")
//                    .replace("<", "\\<");
//            
//            // al posto degli spazi metto le & per fare la ricerca in and e :* per trovare anche le parole che hanno la stringa come radice
//            value = value.trim().replaceAll("\\s+", ":*&");
//        }
//        if (value.trim().isEmpty()) {
//            value = "\\";
//        }
//        String tsCondition = " to_tsquery ('" + ftsConfig + "',$$" + value + ":*$$) @@ " + field;
//        return tsCondition;
//    }

    
}
