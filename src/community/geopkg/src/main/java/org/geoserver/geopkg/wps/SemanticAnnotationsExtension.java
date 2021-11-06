/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

import static java.lang.String.format;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.GeoPkgExtension;
import org.geotools.geopkg.GeoPkgExtensionFactory;
import org.geotools.jdbc.util.SqlUtil;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;

public class SemanticAnnotationsExtension extends GeoPkgExtension {

    static final Logger LOGGER = Logging.getLogger(SemanticAnnotationsExtension.class);

    private static final String NAME = "im_semantic_annotations";
    private static final String SA_TABLE = "gpkgext_semantic_annotations";
    private static final String SA_REFERENCE_TABLE = "gpkgext_sa_reference";
    private static final String DEFINITION =
            "https://gitlab.ogc.org/ogc/t16-d010-geopackage-er/-/blob/master/ER/extensions/13-semantic-annotations.adoc";

    public static class Factory implements GeoPkgExtensionFactory {

        @Override
        public SemanticAnnotationsExtension getExtension(String name, GeoPackage geoPackage) {
            try {
                if (NAME.equals(name)) {
                    return new SemanticAnnotationsExtension(geoPackage);
                }
            } catch (SQLException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Could not initialize the semantic annotations extension",
                        e);
            }

            return null;
        }

        @Override
        public GeoPkgExtension getExtension(Class extensionClass, GeoPackage geoPackage) {
            if (SemanticAnnotationsExtension.class.equals(extensionClass)) {
                try {
                    return new SemanticAnnotationsExtension(geoPackage);
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Could not initialize the styles extension", e);
                }
            }

            return null;
        }
    }

    protected SemanticAnnotationsExtension(GeoPackage geoPackage) throws SQLException {
        super(NAME, DEFINITION, Scope.ReadWrite, geoPackage);
        // ensure the necessary tables are there
        try (Connection cx = getConnection()) {
            if (!extensionExists(cx)) {
                SqlUtil.runScript(
                        PortrayalExtension.class.getResourceAsStream("geopkg_sa_extension.sql"),
                        cx);
            }
        }
    }

    public void addAnnotation(GeoPkgSemanticAnnotation sa) throws SQLException {
        String sql =
                format(
                        "INSERT INTO %s(type, title, description, uri) VALUES(?, ?, ?, ?)",
                        SA_TABLE);
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, sa.getType());
            ps.setString(2, sa.getTitle());
            ps.setString(3, sa.getDescription());
            ps.setString(4, sa.getUri());
            ps.executeUpdate();

            sa.setId(getGeneratedKey(ps));
        }
    }

    public List<GeoPkgSemanticAnnotation> getAnnotationsByType(String type) throws SQLException {
        String sql = format("SELECT id, type, title, description, uri from %s WHERE type = ?");
        List<GeoPkgSemanticAnnotation> result = new ArrayList<>();
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql); ) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapSemanticAnnotation(rs));
                }
            }
        }
        return result;
    }

    public List<GeoPkgSemanticAnnotation> getAnnotationsByURI(String uri) throws SQLException {
        String sql =
                format("SELECT id, type, title, description, uri from %s WHERE uri = ?", SA_TABLE);
        List<GeoPkgSemanticAnnotation> result = new ArrayList<>();
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql); ) {
            ps.setString(1, uri);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapSemanticAnnotation(rs));
                }
            }
        }
        return result;
    }

    public List<GeoPkgSemanticAnnotation> getSemanticAnnotations() throws SQLException {
        String sql = format("SELECT id, type, title, description, uri from %s ");
        List<GeoPkgSemanticAnnotation> result = new ArrayList<>();
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql); ) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapSemanticAnnotation(rs));
                }
            }
        }
        return result;
    }

    private GeoPkgSemanticAnnotation mapSemanticAnnotation(ResultSet rs) throws SQLException {
        return new GeoPkgSemanticAnnotation(
                rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5));
    }

    private boolean extensionExists(Connection cx) throws SQLException {
        try (ResultSet rs =
                cx.getMetaData().getTables(null, null, SA_TABLE, new String[] {"TABLE"})) {
            return rs.next();
        }
    }

    public void addReference(GeoPkgAnnotationReference ref) throws SQLException {
        String sql =
                format(
                        "INSERT INTO %s(table_name, key_column_name, key_value, sa_id) VALUES(?, ?, ?, ?)",
                        SA_REFERENCE_TABLE);
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, ref.getTableName());
            ps.setString(2, ref.getKeyColumnName());
            ps.setObject(3, ref.getKeyValue()); // long, but nullable
            ps.setLong(4, ref.getAnnotation().getId());
            ps.executeUpdate();
        }
    }

    /**
     * Returns references sorted by table_name, key_column_name, key_value
     *
     * @param annotation
     * @return
     * @throws SQLException
     */
    public List<GeoPkgAnnotationReference> getReferencesForAnnotation(
            GeoPkgSemanticAnnotation annotation) throws SQLException {
        String sql =
                format(
                        "SELECT table_name, key_column_name, key_value, sa_id from %s "
                                + "WHERE sa_id = ? "
                                + "ORDER BY table_name, key_column_name, key_value",
                        SA_REFERENCE_TABLE);
        List<GeoPkgAnnotationReference> result = new ArrayList<>();
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql); ) {
            ps.setLong(1, annotation.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapAnnotationReference(rs, annotation));
                }
            }
        }
        return result;
    }

    private GeoPkgAnnotationReference mapAnnotationReference(
            ResultSet rs, GeoPkgSemanticAnnotation annotation) throws SQLException {
        return new GeoPkgAnnotationReference(
                rs.getString(1),
                rs.getString(2),
                Converters.convert(rs.getObject(3), Long.class),
                annotation);
    }
}
