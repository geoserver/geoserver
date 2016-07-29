package org.geoserver.jdbcconfig.internal;

import java.io.IOException;

import org.geoserver.jdbcloader.JDBCLoaderProperties;
import org.geoserver.jdbcloader.JDBCLoaderPropertiesFactoryBean;
import org.geoserver.platform.resource.ResourceStore;

public class JDBCConfigPropertiesFactoryBean extends JDBCLoaderPropertiesFactoryBean {
    
    static final String PREFIX = "jdbcconfig";
    
    /**
     * DDL scripts copied to <data dir>/jdbcconfig_scripts/ on first startup
     */
    private static final String[] SCRIPTS = { "dropdb.h2.sql", "dropdb.mssql.sql",
            "dropdb.mysql.sql", "dropdb.oracle.sql", "dropdb.postgres.sql", "initdb.h2.sql",
            "initdb.mssql.sql", "initdb.mysql.sql", "initdb.oracle.sql", "initdb.postgres.sql" };

    private static final String[] SAMPLE_CONFIGS = { "jdbcconfig.properties.h2",
            "jdbcconfig.properties.postgres" };
    
    public JDBCConfigPropertiesFactoryBean(ResourceStore resourceStore) {
        super(resourceStore, PREFIX);
    }

    @Override
    protected JDBCLoaderProperties createConfig() throws IOException {
        return new JDBCConfigProperties(this);
    }

    @Override
    protected String[] getScripts() {
        return SCRIPTS;
    }

    @Override
    protected String[] getSampleConfigurations() {
        return SAMPLE_CONFIGS;
    }

}
