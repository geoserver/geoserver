/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import static org.geotools.data.postgis.PostgisNGDataStoreFactory.LOOSEBBOX;
import static org.geotools.jdbc.JDBCDataStoreFactory.DATABASE;
import static org.geotools.jdbc.JDBCDataStoreFactory.HOST;
import static org.geotools.jdbc.JDBCDataStoreFactory.PASSWD;
import static org.geotools.jdbc.JDBCDataStoreFactory.PK_METADATA_TABLE;
import static org.geotools.jdbc.JDBCDataStoreFactory.USER;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.wicket.Component;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.data.postgis.PostgisNGJNDIDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;

/**
 * Configuration panel for PostGIS.
 *
 * @author Andrea Aime - OpenGeo
 */
public class PostGISPanel extends AbstractDbPanel {

    JNDIDbParamPanel jndiParamPanel;
    BasicDbParamPanel basicParamPanel;

    public PostGISPanel(String id) {
        super(id);
    }

    @Override
    protected LinkedHashMap<String, Component> buildParamPanels() {
        LinkedHashMap<String, Component> result = new LinkedHashMap<String, Component>();

        int port = 5432;
        String db = System.getProperty("user.name");
        String user = db;

        // basic panel
        basicParamPanel = new BasicDbParamPanel("01", "localhost", port, db, "public", user, true);
        result.put(CONNECTION_DEFAULT, basicParamPanel);

        // jndi panel
        jndiParamPanel = new JNDIDbParamPanel("02", "java:comp/env/jdbc/mydatabase");
        result.put(CONNECTION_JNDI, jndiParamPanel);

        return result;
    }

    @Override
    protected DataStoreFactorySpi fillStoreParams(Map<String, Serializable> params) {
        DataStoreFactorySpi factory;
        params.put(
                JDBCDataStoreFactory.DBTYPE.key, (String) PostgisNGDataStoreFactory.DBTYPE.sample);
        if (CONNECTION_JNDI.equals(connectionType)) {
            factory = new PostgisNGJNDIDataStoreFactory();
            fillInJndiParams(params, jndiParamPanel);
        } else {
            factory = new PostgisNGDataStoreFactory();

            // basic params
            params.put(HOST.key, basicParamPanel.host);
            params.put(PostgisNGDataStoreFactory.PORT.key, basicParamPanel.port);
            params.put(USER.key, basicParamPanel.username);
            params.put(PASSWD.key, basicParamPanel.password);
            params.put(DATABASE.key, basicParamPanel.database);
            params.put(JDBCDataStoreFactory.SCHEMA.key, basicParamPanel.schema);

            // connection pool params
            fillInConnPoolParams(params, basicParamPanel);
        }

        // advanced
        // params.put(NAMESPACE.key, new URI(namespace.getURI()).toString());
        params.put(LOOSEBBOX.key, advancedParamPanel.looseBBox);
        params.put(PK_METADATA_TABLE.key, advancedParamPanel.pkMetadata);

        return factory;
    }

    @Override
    protected AdvancedDbParamPanel buildAdvancedPanel(String id) {
        return new AdvancedDbParamPanel(id, true);
    }
}
