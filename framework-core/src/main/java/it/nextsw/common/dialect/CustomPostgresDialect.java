package it.nextsw.common.dialect;

import static javassist.CtMethod.ConstParameter.integer;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.PatternFunctionDescriptorBuilder;
import org.hibernate.type.spi.TypeConfiguration;

public class CustomPostgresDialect extends PostgreSQLDialect {

    public CustomPostgresDialect() {
        
        super();
//        this.registerFunction("fts_match", new PostgreSQLFullTextSearchFunction());
//        this.registerFunction("array_operation", new PostgresArrayFunctions());
//        this.registerFunction("bitand", new SQLFunctionTemplate(IntegerType.INSTANCE, "(?1 & ?2)"));
//        this.registerFunction("jsonb_contains", new PostgreSQLJsonbFunction());
//        new PostgreSQLJsonbFunction();
//        this.registerFunction("like", new PostgreSQLLikeFunction());
    }

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);
        SqmFunctionRegistry registry = functionContributions.getFunctionRegistry();
        TypeConfiguration types = functionContributions.getTypeConfiguration();
        //BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
        
//        registry.register("fts_match", new PostgreSQLFullTextSearchFunction());
        new PatternFunctionDescriptorBuilder(registry, "bitand", FunctionKind.NORMAL, "(?1 & ?2)")
            .setExactArgumentCount(2)
            .setInvariantType(types.getBasicTypeForJavaType(Integer.class))
            .register();
        registry.register("jsonb_contains", new PostgreSQLJsonbFunction("jsonb_contains"));
        registry.register("fts_match", new PostgreSQLFullTextSearchFunction("fts_match"));
        registry.register("like", new PostgreSQLLikeFunction("like"));
        registry.register("array_operation", new PostgresArrayFunctions("array_operation"));
    }

}
