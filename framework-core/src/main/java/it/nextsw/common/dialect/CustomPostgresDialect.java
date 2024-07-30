package it.nextsw.common.dialect;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.function.SqlFunction;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

public class CustomPostgresDialect extends PostgreSQLDialect {

//    public CustomPostgresDialect() {
//        super();
//        this.registerFunction("fts_match", new PostgreSQLFullTextSearchFunction());
//        this.registerFunction("array_operation", new PostgresArrayFunctions());
//        this.registerFunction("bitand", new SQLFunctionTemplate(IntegerType.INSTANCE, "(?1 & ?2)"));
//        this.registerFunction("jsonb_contains", new PostgreSQLJsonbFunction());
//        this.registerFunction("like", new PostgreSQLLikeFunction());
//    }

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);
        SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
        //TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
        //BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
        
        functionRegistry.register("fts_match", new PostgreSQLFullTextSearchFunction());
        functionRegistry.register("array_operation", function);
        functionRegistry.register("bitand", function);
        functionRegistry.register("jsonb_contains", function);
        functionRegistry.register("like", function);
    }

}
