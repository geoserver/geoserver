/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataAccess;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.Repository;
import org.geotools.util.Converters;
import org.geotools.util.KVP;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Builds {@link JDBCOpenSearchAccess} stores
 *
 * @author Andrea Aime - GeoSolutions
 */
public class JDBCOpenSearchAccessFactory implements DataAccessFactory {

    public static final Param REPOSITORY_PARAM =
            new Param(
                    "repository",
                    Repository.class,
                    "The repository that will provide the store instances",
                    false,
                    null,
                    new KVP(Param.LEVEL, "advanced"));

    public static final Param STORE_PARAM =
            new Param(
                    "store",
                    String.class,
                    "Delegate data store",
                    false,
                    null,
                    new KVP(Param.ELEMENT, String.class));

    /** parameter for database type */
    public static final Param DBTYPE =
            new Param("dbtype", String.class, "Type", true, "opensearch-eo-jdbc");

    /** parameter for namespace of the datastore */
    public static final Param NAMESPACE =
            new Param("namespace", String.class, "Namespace prefix", false);

    private static GeoServer geoServer;

    @Override
    public Map<Key, ?> getImplementationHints() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataAccess<? extends FeatureType, ? extends Feature> createDataStore(
            Map<String, Serializable> params) throws IOException {
        Repository repository = (Repository) REPOSITORY_PARAM.lookUp(params);
        String flatStoreName = (String) STORE_PARAM.lookUp(params);
        String ns = (String) NAMESPACE.lookUp(params);
        Name name = Converters.convert(flatStoreName, Name.class);
        return new JDBCOpenSearchAccess(
                repository, name, ns, GeoServerExtensions.bean(GeoServer.class));
    }

    @Override
    public String getDisplayName() {
        return "JDBC based OpenSearch store";
    }

    @Override
    public String getDescription() {
        return "Builds OpenSearch for EO information out of a suitable relational database source";
    }

    @Override
    public Param[] getParametersInfo() {
        return new Param[] {DBTYPE, REPOSITORY_PARAM, STORE_PARAM, NAMESPACE};
    }

    @Override
    public boolean canProcess(Map<String, Serializable> params) {
        // copied from AbstractDataStoreFactory... really, this code should be somewhere
        // where it can be reused...
        if (params == null) {
            return false;
        }
        Param arrayParameters[] = getParametersInfo();
        for (int i = 0; i < arrayParameters.length; i++) {
            Param param = arrayParameters[i];
            Object value;
            if (!params.containsKey(param.key)) {
                if (param.required) {
                    return false; // missing required key!
                } else {
                    continue;
                }
            }
            try {
                value = param.lookUp(params);
            } catch (IOException e) {
                return false;
            }
            if (value == null) {
                if (param.required) {
                    return (false);
                }
            } else {
                if (!param.type.isInstance(value)) {
                    return false; // value was not of the required type
                }
                if (param.metadata != null) {
                    // check metadata
                    if (param.metadata.containsKey(Param.OPTIONS)) {
                        List<Object> options = (List<Object>) param.metadata.get(Param.OPTIONS);
                        if (options != null && !options.contains(value)) {
                            return false; // invalid option
                        }
                    }
                }
            }
        }

        // dbtype specific check
        String type;
        try {
            type = (String) DBTYPE.lookUp(params);

            if (DBTYPE.sample.equals(type)) {
                return true;
            }

            return false;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
