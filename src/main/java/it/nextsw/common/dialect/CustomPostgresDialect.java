package it.nextsw.common.dialect;

import org.hibernate.dialect.PostgreSQL94Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.IntegerType;

public class CustomPostgresDialect extends PostgreSQL94Dialect {

    public CustomPostgresDialect() {
        super();
        this.registerFunction("fts_match", new PostgreSQLFullTextSearchFunction());
        this.registerFunction("array_operation", new PostgresArrayFunctions());
        this.registerFunction("bitand", new SQLFunctionTemplate(IntegerType.INSTANCE, "(?1 & ?2)"));
    }

}