package org.geoserver.jdbcconfig.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Properties;

import org.geotools.data.DataUtilities;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class JDBCConfigProperties extends Properties {

    //maintain order of keys to prevent writing out in random order
    LinkedHashSet<Object> keys = new LinkedHashSet<Object>();

    //factory
    JDBCConfigPropertiesFactoryBean factory;
    
    String datasourceId = null;

    public JDBCConfigProperties(JDBCConfigPropertiesFactoryBean factory) {
        this.factory = factory;
    }

    @Override
    public synchronized Enumeration<Object> keys() {
        return Collections.enumeration(keys);
    }
    
    @Override
    public synchronized Object put(Object key, Object value) {
        keys.add(key);
        return super.put(key, value);
    }

    public boolean isEnabled() {
        return Boolean.valueOf(getProperty("enabled", "false"));
    }

    public Optional<String> getJdbcUrl() {
        return Optional.fromNullable(fillInPlaceholders(getProperty("jdbcUrl")));
    }

    public void setJdbcUrl(String jdbcUrl) {
        setProperty("jdbcUrl", jdbcUrl);
    }
    public boolean isInitDb() {
        return Boolean.parseBoolean(getProperty("initdb", "false"));
    }
    
    public void setInitDb(boolean initdb) {
        setProperty("initdb", String.valueOf(initdb));
    }
    
    public URL getInitScript() {
        String initScript = fillInPlaceholders(getProperty("initScript"));
        if (initScript == null) {
            return null;
        }
    
        File file = new File(initScript);
        Preconditions.checkState(file.exists(),
            "Init script does not exist: " + file.getAbsolutePath());
    
        return DataUtilities.fileToURL(file);
    }
    
    public boolean isImport() {
        return Boolean.parseBoolean(getProperty("import", "false"));
    }
    
    public void setImport(boolean imprt) {
        setProperty("import", String.valueOf(imprt));
    }

    public void save() throws IOException {
        factory.saveConfig(this);
    }

    String fillInPlaceholders(String value) {
        return value != null ? 
            value.replace("${GEOSERVER_DATA_DIR}", factory.getDataDir().getAbsolutePath()) : value;
    }
    
    public Optional<String> getJndiName() {
        return Optional.fromNullable(getProperty("jndiName"));
    }

    public void setJndiName(String name) {
        setProperty("jndiName", name);
    }

    public String getDatasourceId() {
        return datasourceId;
    }

    public void setDatasourceId(String datasourceId) {
        this.datasourceId = datasourceId;
    }
}