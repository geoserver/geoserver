/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import static org.geotools.jdbc.JDBCDataStoreFactory.DATABASE;
import static org.geotools.jdbc.JDBCDataStoreFactory.HOST;
import static org.geotools.jdbc.JDBCDataStoreFactory.PASSWD;
import static org.geotools.jdbc.JDBCDataStoreFactory.PK_METADATA_TABLE;
import static org.geotools.jdbc.JDBCDataStoreFactory.PORT;
import static org.geotools.jdbc.JDBCDataStoreFactory.USER;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.wicket.Component;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.sqlserver.SQLServerDataStoreFactory;
import org.geotools.data.sqlserver.SQLServerJNDIDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;

public class SQLServerPanel extends AbstractDbPanel {

    JNDIDbParamPanel jndiParamPanel;
    BasicDbParamPanel basicParamPanel;

    public SQLServerPanel(String id) {
        super(id);
    }

    @Override
    protected LinkedHashMap<String, Component> buildParamPanels() {
        LinkedHashMap<String, Component> result = new LinkedHashMap<String, Component>();

        // basic panel
        basicParamPanel = new BasicDbParamPanel("01", "localhost", 4866, true);
        result.put(CONNECTION_DEFAULT, basicParamPanel);

        // jndi param panels
        jndiParamPanel = new JNDIDbParamPanel("03", "java:comp/env/jdbc/mydatabase");
        result.put(CONNECTION_JNDI, jndiParamPanel);

        return result;
    }

    @Override
    protected DataStoreFactorySpi fillStoreParams(Map<String, Serializable> params) {
        DataStoreFactorySpi factory;
        params.put(JDBCDataStoreFactory.DBTYPE.key, "sqlserver");
        if (CONNECTION_JNDI.equals(connectionType)) {
            factory = new SQLServerJNDIDataStoreFactory();

            fillInJndiParams(params, jndiParamPanel);
        } else {
            factory = new SQLServerDataStoreFactory();

            // basic params
            params.put(HOST.key, basicParamPanel.host);
            params.put(PORT.key, basicParamPanel.port);
            params.put(USER.key, basicParamPanel.username);
            params.put(PASSWD.key, basicParamPanel.password);
            params.put(DATABASE.key, basicParamPanel.database);
        }
        if (!CONNECTION_JNDI.equals(connectionType)) {
            // connection pool params common to OCI and default connections
            fillInConnPoolParams(params, basicParamPanel);
        }
        params.put(SQLServerDataStoreFactory.INTSEC.key, false);
        /*
        OtherDbmsParamPanel otherParamsPanel = (OtherDbmsParamPanel) this.otherParamsPanel;
        if(otherParamsPanel.userSchema) {
            params.put(JDBCDataStoreFactory.SCHEMA.key, ((String) params.get(USER.key)).toUpperCase());
        } else {
            params.put(JDBCDataStoreFactory.SCHEMA.key, otherParamsPanel.schema);
        }
        */
        if (basicParamPanel.schema != null) {
            params.put(JDBCDataStoreFactory.SCHEMA.key, basicParamPanel.schema);
        }
        // params.put(NAMESPACE.key, new URI(namespace.getURI()).toString());
        // params.put(SQLServerDataStoreFactory.LOOSEBBOX.key, advancedParamPanel.looseBBox);
        params.put(PK_METADATA_TABLE.key, advancedParamPanel.pkMetadata);
        return factory;
    }
}
