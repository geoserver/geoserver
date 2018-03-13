/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.util.LookupService;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Parameter types refering to external resources.
 * 
 * @author Niels Charlier
 *
 */
@Service
public class ExtTypes {
    private static final Logger LOGGER = Logging.getLogger(ExtTypes.class);
    
    @Autowired
    private LookupService<DbSource> dbSources;
    
    @Autowired
    private LookupService<ExternalGS> extGeoservers;
    
    @Autowired
    private GeoServer geoServer;

    public final ParameterType dbName = new ParameterType() {

        @Override
        public List<String> getDomain(List<String> dependsOnRawValues) {
            return new ArrayList<String>(dbSources.names());
        }

        @Override
        public DbSource parse(String value, List<String> dependsOnRawValues) {
            return dbSources.get(value);
        }

    };
    
    public final ParameterType tableName() {
        return new ParameterType() {
            private Set<String> getTables(String databaseName) {
                Set<String> tables = new TreeSet<String>();
                tables.add(""); //custom value is possible here
                if (databaseName != null) {
                    DbSource ds = dbSources.get(databaseName);
                    if (ds != null) {
                        try {
                            Connection conn = ds.getDataSource().getConnection();
                            DatabaseMetaData md = conn.getMetaData();
                            ResultSet rs = md.getTables(null, ds.getSchema(), "%", 
                                    new String[]{"TABLE", "VIEW"});
                            while (rs.next()) {
                                if (ds.getSchema() != null || rs.getString(2) == null) {
                                    tables.add(rs.getString(3));
                                } else {
                                    tables.add(rs.getString(2) + "." + rs.getString(3));
                                }
                            }
                        } catch (SQLException e) {
                            LOGGER.log(Level.WARNING, "Failed to retrieve tables from data source " + databaseName, e);
                        }
                    }
                }
                return tables;
            }        

            @Override
            public List<String> getDomain(List<String> dependsOnRawValues) {
                return new ArrayList<String>(getTables(dependsOnRawValues.get(0)));
            }
            
            @Override
            public boolean validate(String value, List<String> dependsOnRawValues) {
                //since the table may not yet exist  (could be result of other task
                //do not validate its existence.
                return true; 
            }
    
            @Override
            public Object parse(String value, List<String> dependsOnRawValues) {
                return new DbTableImpl(dbSources.get(dependsOnRawValues.get(0)), value);
            }
    
        };
    };
    
    public final ParameterType extGeoserver = new ParameterType() {

        @Override
        public List<String> getDomain(List<String> dependsOnRawValues) {
            return new ArrayList<String>(extGeoservers.names());
        }

        @Override
        public ExternalGS parse(String value, List<String> dependsOnRawValues) {
            return extGeoservers.get(value);
        }

    };
    
    public final ParameterType internalLayer = new ParameterType() {

        @Override
        public List<String> getDomain(List<String> dependsOnRawValues) {
            List<String> layers = new ArrayList<>();
            for (LayerInfo layer : geoServer.getCatalog().getLayers()) {
                layers.add(layer.prefixedName());
            }
            return layers;
        }

        @Override
        public LayerInfo parse(String value, List<String> dependsOnRawValues) {
            return geoServer.getCatalog().getLayerByName(value);
        }
        
        @Override
        public List<String> getActions() {
            return Collections.singletonList("LayerEdit");
        }
        
    };
    
    public final ParameterType layerName = new ParameterType() {
        
        @Override
        public List<String> getDomain(List<String> dependsOnRawValues) {
            /*List<String> layers = new ArrayList<>();
            layers.add(""); //custom value is possible here
            for (LayerInfo layer : geoServer.getCatalog().getLayers()) {
                layers.add(layer.prefixedName());
            }
            return layers;*/
            return null;
        }

        @Override
        public Name parse(String value, List<String> dependsOnRawValues) {
            int colon = value.indexOf(':') ;
            NamespaceInfo ni;
            if (colon >= 0) {
                ni = geoServer.getCatalog().getNamespaceByPrefix(value.substring(0, colon));
            } else {
                ni = geoServer.getCatalog().getDefaultNamespace();
            }
            return new NameImpl(ni == null ? null : ni.getURI(), value.substring(colon + 1));
        }
        
        @Override
        public boolean validate(String value, List<String> dependsOnRawValues) {
            int colon = value.indexOf(':');
            if (colon >= 0) {
                return geoServer.getCatalog().getNamespaceByPrefix(value.substring(0, colon)) != null;
            }
            return true; 
        }

        @Override
        public List<String> getActions() {
            return Collections.singletonList("LayerEdit");
        }

    };
}

