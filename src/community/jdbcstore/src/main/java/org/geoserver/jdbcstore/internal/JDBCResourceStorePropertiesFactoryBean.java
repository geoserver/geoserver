/* Copyright (c) 2015 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcstore.internal;

import java.io.IOException;

import org.geoserver.jdbcloader.JDBCLoaderProperties;
import org.geoserver.jdbcloader.JDBCLoaderPropertiesFactoryBean;
import org.geoserver.platform.resource.ResourceStore;

/**
 * Factory to load configuration information for JDBCResourceStore
 * 
 * @author Kevin Smith, Boundless
 * @author Niels Charlier
 *
 */
public class JDBCResourceStorePropertiesFactoryBean extends JDBCLoaderPropertiesFactoryBean {
    
    private static final String PREFIX = "jdbcstore";
    
    /**
     * DDL scripts copied to <data dir>/jdbcstore/scripts/ on first startup
     */
    private static final String[] SCRIPTS = { "drop.h2.sql", "drop.postgres.sql", 
        "init.h2.sql", "init.postgres.sql" };

    private static final String[] SAMPLE_CONFIGS = { "jdbcstore.properties.h2",
            "jdbcstore.properties.postgres" };
    

    public JDBCResourceStorePropertiesFactoryBean(ResourceStore resourceStore) {
        super(resourceStore, PREFIX);
    }

    @Override
    protected JDBCLoaderProperties createConfig() throws IOException {
        return new JDBCResourceStoreProperties(this);
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
