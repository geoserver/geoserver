/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcloader;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Properties;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;

public class JDBCLoaderProperties extends Properties {

    private static final long serialVersionUID = -6758388267074914346L;

    // maintain order of keys to prevent writing out in random order
    LinkedHashSet<Object> keys = new LinkedHashSet<Object>();

    // factory
    JDBCLoaderPropertiesFactoryBean factory;

    String datasourceId = null;

    public JDBCLoaderProperties(JDBCLoaderPropertiesFactoryBean factory) {
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

    public Resource getInitScript() {
        String initScript = getProperty("initScript");
        if (initScript == null) {
            return null;
        }

        Resource resource = Resources.fromPath(initScript, factory.getDataDir());
        Preconditions.checkState(
                Resources.exists(resource), "Init script does not exist: " + resource.path());

        return resource;
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
        return value != null
                ? value.replace("${GEOSERVER_DATA_DIR}", factory.getDataDirStr())
                : value;
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
