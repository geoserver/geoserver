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
import org.geotools.util.logging.Logging;

/**
 * Generalized tables extensions, for the moment, supporting only the write side (would be
 * interesting to have it on the read side as well, but would need to be core)
 */
public class GeneralizedTablesExtension extends GeoPkgExtension {

    static final Logger LOGGER =
            Logging.getLogger(org.geoserver.geopkg.wps.GeneralizedTablesExtension.class);

    private static final String NAME = "tb16_generalized";
    private static final String GT_TABLE = "gpkgext_generalized";
    private static final String DEFINITION =
            "https://gitlab.ogc.org/ogc/t16-d010-geopackage-er/-/tree/master/ER";

    public static class Factory implements GeoPkgExtensionFactory {

        @Override
        public GeneralizedTablesExtension getExtension(String name, GeoPackage geoPackage) {
            try {
                if (NAME.equals(name)) {
                    return new GeneralizedTablesExtension(geoPackage);
                }
            } catch (SQLException e) {
                LOGGER.log(
                        Level.WARNING, "Could not initialize the generalized tables extension", e);
            }

            return null;
        }

        @Override
        public GeoPkgExtension getExtension(
                @SuppressWarnings("rawtypes") Class extensionClass, GeoPackage geoPackage) {
            if (GeneralizedTablesExtension.class.equals(extensionClass)) {
                try {
                    return new GeneralizedTablesExtension(geoPackage);
                } catch (SQLException e) {
                    LOGGER.log(
                            Level.WARNING,
                            "Could not initialize the generalized tables extension",
                            e);
                }
            }

            return null;
        }
    }

    protected GeneralizedTablesExtension(GeoPackage geoPackage) throws SQLException {
        super(NAME, DEFINITION, GeoPkgExtension.Scope.ReadWrite, geoPackage);
        // ensure the necessary tables are there
        try (Connection cx = getConnection()) {
            if (!extensionExists(cx)) {
                SqlUtil.runScript(
                        PortrayalExtension.class.getResourceAsStream(
                                "geopkg_generalized_extension.sql"),
                        cx);
            }
        }
    }

    private boolean extensionExists(Connection cx) throws SQLException {
        try (ResultSet rs =
                cx.getMetaData().getTables(null, null, GT_TABLE, new String[] {"TABLE"})) {
            return rs.next();
        }
    }

    /**
     * Registers a generalized table TODO: move here the actual data copying? Do we want to register
     * the filter too?
     *
     * @param orginalLayerName
     * @param overviewName
     */
    public void addTable(GeneralizedTable generalized) throws SQLException {
        // register into the generalized tables
        String sql =
                format(
                        "INSERT INTO %s(primary_table, generalized_table, distance, provenance) VALUES(?, ?, ?, ?)",
                        GT_TABLE);
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, generalized.getPrimaryTable());
            ps.setString(2, generalized.getGeneralizedTable());
            ps.setDouble(3, generalized.getDistance());
            ps.setString(4, generalized.getProvenance());
            ps.executeUpdate();
        }

        // but also among the extensions
        sql =
                format(
                        "INSERT INTO gpkg_extensions \n"
                                + "VALUES \n"
                                + "  (\n"
                                + "    ?, null, ?, \n"
                                + "    ?, \n"
                                + "    'read-write'\n"
                                + "  );");
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, generalized.getGeneralizedTable());
            ps.setString(2, NAME);
            ps.setString(3, DEFINITION);
            ps.executeUpdate();
        }
    }

    public List<GeneralizedTable> getGeneralizedTables(String primaryTable) throws SQLException {
        String sql =
                format(
                        "SELECT primary_table, generalized_table, distance, provenance from %s "
                                + "WHERE primary_table = ? "
                                + "ORDER BY distance ASC",
                        GT_TABLE);
        List<GeneralizedTable> result = new ArrayList<>();
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql); ) {
            ps.setString(1, primaryTable);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapGeneralizedTable(rs));
                }
            }
        }
        return result;
    }

    private GeneralizedTable mapGeneralizedTable(ResultSet rs) throws SQLException {
        GeneralizedTable gt =
                new GeneralizedTable(
                        rs.getString("primary_table"),
                        rs.getString("generalized_table"),
                        rs.getDouble("distance"));
        gt.setProvenance(rs.getString("provenance"));
        return gt;
    }
}
