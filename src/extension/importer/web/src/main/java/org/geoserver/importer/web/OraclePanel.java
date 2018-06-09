/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import static org.geotools.data.oracle.OracleNGDataStoreFactory.DATABASE;
import static org.geotools.data.oracle.OracleNGDataStoreFactory.HOST;
import static org.geotools.data.oracle.OracleNGOCIDataStoreFactory.ALIAS;
import static org.geotools.jdbc.JDBCDataStoreFactory.PASSWD;
import static org.geotools.jdbc.JDBCDataStoreFactory.PK_METADATA_TABLE;
import static org.geotools.jdbc.JDBCDataStoreFactory.USER;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.oracle.OracleNGDataStoreFactory;
import org.geotools.data.oracle.OracleNGJNDIDataStoreFactory;
import org.geotools.data.oracle.OracleNGOCIDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;

public class OraclePanel extends AbstractDbPanel {

    protected static final String CONNECTION_OCI = "OCI";

    JNDIDbParamPanel jndiParamPanel;
    BasicDbParamPanel basicParamPanel;
    OCIParamPanel ociParamPanel;

    public OraclePanel(String id) {
        super(id);
    }

    @Override
    protected LinkedHashMap<String, Component> buildParamPanels() {
        LinkedHashMap<String, Component> result = new LinkedHashMap<String, Component>();

        // basic panel
        basicParamPanel = new BasicDbParamPanel("01", "localhost", 1521, true);
        result.put(CONNECTION_DEFAULT, basicParamPanel);

        // oci one
        ociParamPanel = new OCIParamPanel("02");
        result.put(CONNECTION_OCI, ociParamPanel);

        // jndi param panels
        jndiParamPanel = new JNDIDbParamPanel("03", "java:comp/env/jdbc/mydatabase");
        result.put(CONNECTION_JNDI, jndiParamPanel);

        return result;
    }

    @Override
    protected DataStoreFactorySpi fillStoreParams(Map<String, Serializable> params) {
        DataStoreFactorySpi factory;
        params.put(JDBCDataStoreFactory.DBTYPE.key, "oracle");
        if (CONNECTION_JNDI.equals(connectionType)) {
            factory = new OracleNGJNDIDataStoreFactory();

            fillInJndiParams(params, jndiParamPanel);
        } else if (CONNECTION_OCI.equals(connectionType)) {
            factory = new OracleNGOCIDataStoreFactory();

            params.put(ALIAS.key, ociParamPanel.alias);
            params.put(USER.key, ociParamPanel.username);
            params.put(PASSWD.key, ociParamPanel.password);
        } else {
            factory = new OracleNGDataStoreFactory();

            // basic params
            params.put(HOST.key, basicParamPanel.host);
            params.put(OracleNGDataStoreFactory.PORT.key, basicParamPanel.port);
            params.put(USER.key, basicParamPanel.username);
            params.put(PASSWD.key, basicParamPanel.password);
            params.put(DATABASE.key, basicParamPanel.database);
        }
        if (!CONNECTION_JNDI.equals(connectionType)) {
            // connection pool params common to OCI and default connections
            fillInConnPoolParams(params, basicParamPanel);
        }

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
        params.put(OracleNGDataStoreFactory.LOOSEBBOX.key, advancedParamPanel.looseBBox);
        params.put(PK_METADATA_TABLE.key, advancedParamPanel.pkMetadata);
        return factory;
    }

    static class OCIParamPanel extends Panel {
        String alias;
        String username;
        String password;

        public OCIParamPanel(String id) {
            super(id);

            add(new TextField("alias", new PropertyModel(this, "alias")).setRequired(true));
            add(new TextField("username", new PropertyModel(this, "username")).setRequired(true));
            add(
                    new PasswordTextField("password", new PropertyModel(this, "password"))
                            .setResetPassword(false));
        }
    }
}
