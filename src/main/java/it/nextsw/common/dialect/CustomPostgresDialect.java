package it.nextsw.common.dialect;

import org.hibernate.dialect.PostgreSQL94Dialect;

public class CustomPostgresDialect extends PostgreSQL94Dialect {

    public CustomPostgresDialect() {
        super();
        this.registerFunction("fts_match", new PostgreSQLFullTextSearchFunction());
        this.registerFunction("array_operation", new PostgresArrayFunctions());
    }

}