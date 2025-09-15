/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import java.io.Serial;
import org.geoserver.jdbcloader.JDBCLoaderProperties;

public class JDBCConfigProperties extends JDBCLoaderProperties {

    @Serial
    private static final long serialVersionUID = -1808911356328897645L;

    public JDBCConfigProperties(JDBCConfigPropertiesFactoryBean factory) {
        super(factory);
    }

    // jdbcconfig specific properties  may go here.

    public boolean isRepopulate() {
        return Boolean.parseBoolean(getProperty("repopulate", "false"));
    }

    public void setRepopulate(boolean initdb) {
        setProperty("repopulate", String.valueOf(initdb));
    }
}
