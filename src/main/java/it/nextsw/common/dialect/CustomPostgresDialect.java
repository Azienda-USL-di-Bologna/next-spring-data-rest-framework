package it.nextsw.common.dialect;

import org.hibernate.dialect.PostgreSQL94Dialect;

public class CustomPostgresDialect extends PostgreSQL94Dialect {

    public CustomPostgresDialect() {
        registerFunction("fts_match", new PostgreSQLFullTextSearchFunction());
    }

}