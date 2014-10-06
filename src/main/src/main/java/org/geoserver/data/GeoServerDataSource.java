/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSource;
import org.geoserver.config.GeoServerDataDirectory;

/**
 * A datasource that is user configurable via properties file stored in the 
 * geoserver data directory.
 * <p>
 * Instances of this class are defined in a spring context as follows:
 * <pre>
 *   &lt;bean id="myDataSource" class="org.geoserver.data.GeoServerDataSource">
 *     &lt;property name="dataDirectory" ref="dataDirectory"/>
       &lt;property name="file" value="mydatasource.properties"/>
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
 * </p>
 * 
 * Note that any property values can contain "${GEOSERVER_DATA_DIR}" and it will be expanded out
 * to the absolute path of the geoserver data directory. 
 * 
 * @author Justin Deoliveira, OpenGeo
 *
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
        if(getDriverClassName() == null) {
            synchronized(this) {
                if (getDriverClassName() == null) {
                    initializeDataSource();
                }
            }
        }
        return super.getConnection();
    }

    void initializeDataSource() {
        try {
            File dbprops = new File(dataDirectory.root(), file);
            
            Properties db = new Properties();
            if (!dbprops.exists()) {
                if (dbprops.getParentFile().exists()) {
                    dbprops.getParentFile().mkdirs();
                }
                
                //use the default parameters and save them out
                FileOutputStream fout = new FileOutputStream(dbprops);
                try {
                    defaultParameters.store(fout, null);
                } 
                finally {
                    fout.close();
                }
                db.putAll(defaultParameters);
            }
            else {
                FileInputStream in = new FileInputStream(dbprops);
                db.load(in);
                in.close();
            }
            
            //TODO: check for nulls
            setDriverClassName(db.getProperty("driver"));
            setUrl(getURL(db));
            
            if (db.containsKey("username")) {
                setUsername(db.getProperty("username"));
            }
            if (db.containsKey("password")) {
                setPassword(db.getProperty("password"));
            }
            
            //TODO: make other parameters configurable
            setMinIdle(1);
            setMaxActive(4);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error setting up the datas source", e);
        }
    }
    
    String getURL(Properties db) {
        return db.getProperty("url").replace("%GEOSERVER_DATA_DIR%", dataDirectory.root().getAbsolutePath());
    }
}
