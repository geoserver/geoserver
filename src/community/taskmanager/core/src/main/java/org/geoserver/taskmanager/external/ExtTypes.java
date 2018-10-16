/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.taskmanager.external.impl.DbTableImpl;
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
 */
@Service
public class ExtTypes {
    private static final Logger LOGGER = Logging.getLogger(ExtTypes.class);

    @Autowired private LookupService<DbSource> dbSources;

    @Autowired private LookupService<ExternalGS> extGeoservers;

    @Autowired private LookupService<FileService> fileServices;

    @Autowired private GeoServer geoServer;

    public final ParameterType dbName =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return new ArrayList<String>(dbSources.names());
                }

                @Override
                public DbSource parse(String value, List<String> dependsOnRawValues) {
                    return dbSources.get(value);
                }
            };

    public final ParameterType tableName =
            new ParameterType() {
                private Set<String> getTables(String databaseName) {
                    SortedSet<String> tables = new TreeSet<String>();
                    tables.add(""); // custom value is possible here
                    if (databaseName != null) {
                        DbSource ds = dbSources.get(databaseName);
                        if (ds != null) {
                            try {
                                try (Connection conn = ds.getDataSource().getConnection()) {
                                    DatabaseMetaData md = conn.getMetaData();
                                    try (ResultSet rs =
                                            md.getTables(
                                                    null,
                                                    ds.getSchema(),
                                                    "%",
                                                    new String[] {"TABLE", "VIEW"})) {
                                        while (rs.next()) {
                                            if (ds.getSchema() != null || rs.getString(2) == null) {
                                                tables.add(rs.getString(3));
                                            } else {
                                                tables.add(rs.getString(2) + "." + rs.getString(3));
                                            }
                                        }
                                    }
                                }
                            } catch (SQLException e) {
                                LOGGER.log(
                                        Level.WARNING,
                                        "Failed to retrieve tables from data source "
                                                + databaseName,
                                        e);
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
                    if (dependsOnRawValues.size() < 1) {
                        throw new IllegalArgumentException(
                                "tableName parameter must be dependent on database.");
                    }
                    // since the table may not yet exist, do not validate its existence.
                    return true;
                }

                @Override
                public Object parse(String value, List<String> dependsOnRawValues) {
                    if (dependsOnRawValues.size() < 1) {
                        throw new IllegalArgumentException(
                                "tableName parameter must be dependent on database.");
                    }
                    return new DbTableImpl(dbSources.get(dependsOnRawValues.get(0)), value);
                }
            };

    public final ParameterType extGeoserver =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return new ArrayList<String>(extGeoservers.names());
                }

                @Override
                public ExternalGS parse(String value, List<String> dependsOnRawValues) {
                    return extGeoservers.get(value);
                }
            };

    public final ParameterType internalLayer =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    SortedSet<String> layers = new TreeSet<>();
                    for (LayerInfo layer : geoServer.getCatalog().getLayers()) {
                        layers.add(layer.prefixedName());
                    }
                    return new ArrayList<String>(layers);
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

    public final ParameterType name =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return null;
                }

                @Override
                public Name parse(String value, List<String> dependsOnRawValues) {
                    int colon = value.indexOf(':');
                    NamespaceInfo ni;
                    if (colon >= 0) {
                        ni = geoServer.getCatalog().getNamespaceByPrefix(value.substring(0, colon));
                    } else {
                        ni = geoServer.getCatalog().getDefaultNamespace();
                    }
                    return new NameImpl(
                            ni == null ? null : ni.getURI(), value.substring(colon + 1));
                }

                @Override
                public boolean validate(String value, List<String> dependsOnRawValues) {
                    int colon = value.indexOf(':');
                    if (colon >= 0) {
                        return geoServer
                                        .getCatalog()
                                        .getNamespaceByPrefix(value.substring(0, colon))
                                != null;
                    }
                    return true;
                }
            };

    public final ParameterType fileService =
            new ParameterType() {
                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return new ArrayList<String>(fileServices.names());
                }

                @Override
                public FileService parse(String value, List<String> dependsOnRawValues) {
                    return fileServices.get(value);
                }
            };

    public final ParameterType file(boolean mustExist, boolean canUpload) {
        return new ParameterType() {
            @Override
            public List<String> getDomain(List<String> dependsOnRawValues) {
                return null;
            }

            @Override
            public FileReference parse(String value, List<String> dependsOnRawValues) {
                if (dependsOnRawValues.size() < 1) {
                    throw new IllegalArgumentException(
                            "file parameter must be dependent on file service.");
                }
                FileService fileService = fileServices.get(dependsOnRawValues.get(0));
                if (dependsOnRawValues.size() >= 2
                        && "true".equalsIgnoreCase(dependsOnRawValues.get(1))) {
                    value = FileService.versioned(value);
                }
                FileReference ref = fileService.getVersioned(value);
                if (mustExist) {
                    try {
                        if (!fileService.checkFileExists(ref.getLatestVersion())) {
                            return null;
                        }
                    } catch (IOException e) {
                        return null;
                    }
                }
                return ref;
            }

            @Override
            public boolean validate(String value, List<String> dependsOnRawValues) {
                if (dependsOnRawValues.size() < 1) {
                    throw new IllegalArgumentException(
                            "file parameter must be dependent on file service.");
                }
                // since the file may not yet exist at configuration
                // do not validate its existence
                return true;
            }

            @Override
            public List<String> getActions() {
                return canUpload
                        ? Collections.singletonList("FileUpload")
                        : Collections.emptyList();
            }
        };
    }
}
