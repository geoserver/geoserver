package org.geoserver.monitor.hib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServerDataDirectory;

public class MonitoringDataSource extends BasicDataSource {

    GeoServerDataDirectory dataDirectory;

    public void setDataDirectory(GeoServerDataDirectory dataDir) {
        this.dataDirectory = dataDir;
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
            File monitoringDir = dataDirectory.findOrCreateDataDir("monitoring");
            File dbprops = new File(monitoringDir, "db.properties");
            Properties db = new Properties();
        
            if (!dbprops.exists()) {
                //use a default, and copy the template over
                InputStream in = getClass().getResourceAsStream("db.properties");
                IOUtils.copy(in, new FileOutputStream(dbprops));
                
                db.load(getClass().getResourceAsStream("db.properties"));
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
            throw new RuntimeException("Unexpected error setting up the monitoring H2 database", e);
        }
    }
    
    String getURL(Properties db) {
        return db.getProperty("url").replace("${GEOSERVER_DATA_DIR}", dataDirectory.root().getAbsolutePath());
    }
}
