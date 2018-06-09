package org.geoserver.jdbcconfig.internal;

import org.geoserver.jdbcloader.JDBCLoaderProperties;

public class JDBCConfigProperties extends JDBCLoaderProperties {

    private static final long serialVersionUID = -1808911356328897645L;

    public JDBCConfigProperties(JDBCConfigPropertiesFactoryBean factory) {
        super(factory);
    }

    // jdbcconfig specific properties  may go here.

}
