package it.nextsw.common.dialect;

import java.util.List;
import org.hibernate.QueryException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 *
 * @author gdm
 */
public class PostgreSQLFullTextSearchFunction implements SqmFunctionDescriptor {

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
    public BasicTypeReference<Boolean> getReturnType(Type firstArgumentType, Mapping mapping) throws QueryException {
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
            value = value.trim().replaceAll("\\s+", ":*&");
        }
        if (value.trim().isEmpty()) {
            value = "\\";
        }
        String tsCondition = " to_tsquery ('" + ftsConfig + "',$$" + value + ":*$$) @@ " + field;
        return tsCondition;
    }

    @Override
    public <T> SelfRenderingSqmFunction<T> generateSqmExpression(List<? extends SqmTypedNode<?>> arguments, ReturnableType<T> impliedResultType, QueryEngine queryEngine) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public <T> SelfRenderingSqmFunction<T> generateAggregateSqmExpression(List<? extends SqmTypedNode<?>> arguments, SqmPredicate filter, ReturnableType<T> impliedResultType, QueryEngine queryEngine) {
        return SqmFunctionDescriptor.super.generateAggregateSqmExpression(arguments, filter, impliedResultType, queryEngine); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public <T> SelfRenderingSqmFunction<T> generateOrderedSetAggregateSqmExpression(List<? extends SqmTypedNode<?>> arguments, SqmPredicate filter, SqmOrderByClause withinGroupClause, ReturnableType<T> impliedResultType, QueryEngine queryEngine) {
        return SqmFunctionDescriptor.super.generateOrderedSetAggregateSqmExpression(arguments, filter, withinGroupClause, impliedResultType, queryEngine); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public <T> SelfRenderingSqmFunction<T> generateWindowSqmExpression(List<? extends SqmTypedNode<?>> arguments, SqmPredicate filter, Boolean respectNulls, Boolean fromFirst, ReturnableType<T> impliedResultType, QueryEngine queryEngine) {
        return SqmFunctionDescriptor.super.generateWindowSqmExpression(arguments, filter, respectNulls, fromFirst, impliedResultType, queryEngine); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public <T> SelfRenderingSqmFunction<T> generateSqmExpression(SqmTypedNode<?> argument, ReturnableType<T> impliedResultType, QueryEngine queryEngine) {
        return SqmFunctionDescriptor.super.generateSqmExpression(argument, impliedResultType, queryEngine); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public <T> SelfRenderingSqmFunction<T> generateSqmExpression(ReturnableType<T> impliedResultType, QueryEngine queryEngine) {
        return SqmFunctionDescriptor.super.generateSqmExpression(impliedResultType, queryEngine); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public boolean alwaysIncludesParentheses() {
        return SqmFunctionDescriptor.super.alwaysIncludesParentheses(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public String getSignature(String name) {
        return SqmFunctionDescriptor.super.getSignature(name); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public FunctionKind getFunctionKind() {
        return SqmFunctionDescriptor.super.getFunctionKind(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public ArgumentsValidator getArgumentsValidator() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}
