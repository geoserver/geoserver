/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.apache.commons.dbcp.BasicDataSource;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;

/**
 * A datasource that is user configurable via properties file stored in the geoserver data
 * directory.
 *
 * <p>Instances of this class are defined in a spring context as follows:
 *
 * <pre>
 *   &lt;bean id="myDataSource" class="org.geoserver.data.GeoServerDataSource">
 *     &lt;property name="dataDirectory" ref="dataDirectory"/>
 * &lt;property name="file" value="mydatasource.properties"/>
 *     &lt;property name="defaultParameters">
 *       &lt;props>
 *         &lt;prop key="driver">org.h2.Driver&lt;/prop>
 *         &lt;prop key="url">jdbc:h2:file:%GEOSERVER_DATA_DIR%/mydb&lt;/prop>
 *         &lt;prop key="username">foo&lt;/prop>
 *         &lt;prop key="password">bar&lt;/prop>
 *       &lt;/props>
 *     &lt;/property>
 *   &lt;/bean>
 * </pre>
 *
 * Note that any property values can contain "${GEOSERVER_DATA_DIR}" and it will be expanded out to
 * the absolute path of the geoserver data directory.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerDataSource extends BasicDataSource {
    GeoServerDataDirectory dataDirectory;

    String file;
    Properties defaultParameters;

    public void setDataDirectory(GeoServerDataDirectory dataDir) {
        this.dataDirectory = dataDir;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setDefaultParameters(Properties defaultParameters) {
        this.defaultParameters = defaultParameters;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (getDriverClassName() == null) {
            synchronized (this) {
                if (getDriverClassName() == null) {
                    initializeDataSource();
                }
            }
        }
        return super.getConnection();
    }

    void initializeDataSource() {
        try {
            Resource dbprops = dataDirectory.get(file);

            Properties db = new Properties();
            if (dbprops.getType() != Type.RESOURCE) {
                // use the default parameters and save them out
                OutputStream fout = dbprops.out();
                try {
                    defaultParameters.store(fout, null);
                } finally {
                    fout.close();
                }
                db.putAll(defaultParameters);
            } else {
                InputStream in = dbprops.in();
                db.load(in);
                in.close();
            }

            // TODO: check for nulls
            setDriverClassName(db.getProperty("driver"));
            setUrl(getURL(db));

            if (db.containsKey("username")) {
                setUsername(db.getProperty("username"));
            }
            if (db.containsKey("password")) {
                setPassword(db.getProperty("password"));
            }

            // TODO: make other parameters configurable
            setMinIdle(1);
            setMaxActive(4);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error setting up the datas source", e);
        }
    }

    String getURL(Properties db) {
        return db.getProperty("url")
                .replace("%GEOSERVER_DATA_DIR%", dataDirectory.root().getAbsolutePath());
    }
}
