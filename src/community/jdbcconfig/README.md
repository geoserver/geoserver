# Testing

By default, the tests will run against an H2 database.

To test against another database, activate the appropriate profile and provide
the following system properties prefixed by `jdbcconfig`.`database`.

* dbUser
* dbPasswd
* connectionUrl

For example:

    jdbcconfig.oracle.dbUser=system
    jdbcconfig.oracle.dbPasswd=oracle
    jdbcconfig.postgres.dbUser=postgres
    jdbcconfig.postgres.dbPasswd=postgres

To run a single test method, see CatalogImplWithJDBCFacadeTest.main for an
example and explanation of why this is needed.

