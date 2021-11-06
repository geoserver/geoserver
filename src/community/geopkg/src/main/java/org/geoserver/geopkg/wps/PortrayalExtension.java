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
 * The portrayal extension allows to export styles and symbols. Style can then be associated to
 * tables via the semantic annotations extension, using the 'style' type. Specification at
 * https://gitlab.ogc.org/ogc/t16-d010-geopackage-er/-/blob/master/ER/extensions/5-portrayal.adoc
 * (closed repo for the time being)
 */
public class PortrayalExtension extends GeoPkgExtension {

    static final Logger LOGGER = Logging.getLogger(PortrayalExtension.class);

    /** The type used in semantic annotations to link styles with tables */
    public static final String SA_TYPE_STYLE = "style";

    public static final String STYLES_TABLE = "gpkgext_styles";
    private static final String STYLESHEETS_TABLE = "gpkgext_stylesheets";
    private static final String SYMBOLS_TABLE = "gpkgext_symbols";
    private static final String SYMBOL_IMAGES_TABLE = "gpkgext_symbol_images";
    private static final String SYMBOL_CONTENTS_TABLE = "gpkgext_symbol_content";
    private static final String NAME = "im_portrayal";
    private static final String DEFINITION =
            "https://gitlab.ogc.org/ogc/t16-d010-geopackage-er/-/blob/master/ER/extensions/5"
                    + "-portrayal.adoc";

    public static class Factory implements GeoPkgExtensionFactory {

        @Override
        public PortrayalExtension getExtension(String name, GeoPackage geoPackage) {
            try {
                if (NAME.equals(name)) {
                    return new PortrayalExtension(geoPackage);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Could not initialize the styles extension", e);
            }

            return null;
        }

        @Override
        public GeoPkgExtension getExtension(Class extensionClass, GeoPackage geoPackage) {
            if (PortrayalExtension.class.equals(extensionClass)) {
                try {
                    return new PortrayalExtension(geoPackage);
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Could not initialize the styles extension", e);
                }
            }

            return null;
        }
    }

    protected PortrayalExtension(GeoPackage geoPackage) throws SQLException {
        super(NAME, DEFINITION, Scope.ReadWrite, geoPackage);
        // ensure the necessary tables are there
        try (Connection cx = getConnection()) {
            if (!extensionExists(cx)) {
                SqlUtil.runScript(
                        PortrayalExtension.class.getResourceAsStream(
                                "geopkg_portrayal_extension.sql"),
                        cx);
            }
        }
    }

    private boolean extensionExists(Connection cx) throws SQLException {
        try (ResultSet rs =
                cx.getMetaData().getTables(null, null, STYLES_TABLE, new String[] {"TABLE"})) {
            return rs.next();
        }
    }

    public GeoPkgStyle getStyle(String styleName) throws SQLException {
        String sql = format("SELECT *" + " FROM %s WHERE style = ?", STYLES_TABLE);
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql); ) {
            ps.setString(1, styleName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapStyle(rs);
                }
            }
        }

        return null;
    }

    public List<GeoPkgStyle> getStyles() throws SQLException {
        String sql = format("SELECT *" + " FROM %s", STYLES_TABLE);
        List<GeoPkgStyle> result = new ArrayList<>();
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                GeoPkgStyle style = mapStyle(rs);
                result.add(style);
            }
        }

        return result;
    }

    public void addStyle(GeoPkgStyle gs) throws SQLException {
        String sql =
                format("INSERT INTO %s(style, description, uri) VALUES(?, ?, ?)", STYLES_TABLE);
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, gs.getStyle());
            ps.setString(2, gs.getDescription());
            ps.setString(3, gs.getUri());
            ps.executeUpdate();

            gs.setId(getGeneratedKey(ps));
        }
    }

    private GeoPkgStyle mapStyle(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String style = rs.getString("style");
        String description = rs.getString("description");
        String uri = rs.getString("uri");
        GeoPkgStyle gps = new GeoPkgStyle(id, style, uri);
        gps.setDescription(description);
        return gps;
    }

    public void addStylesheet(GeoPkgStyleSheet styleSheet) throws SQLException {
        String sql =
                format(
                        "INSERT INTO %s(style_id, format, stylesheet) VALUES(?, ?, ?)",
                        STYLESHEETS_TABLE);
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setLong(1, styleSheet.getStyle().getId());
            ps.setString(2, styleSheet.getFormat());
            ps.setString(3, styleSheet.getStylesheet());
            ps.executeUpdate();
        }
    }

    public List<GeoPkgStyleSheet> getStylesheets(GeoPkgStyle gs) throws SQLException {
        String sql = format("SELECT *" + " FROM %s where style_id = ?", STYLESHEETS_TABLE);
        List<GeoPkgStyleSheet> result = new ArrayList<>();
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql); ) {
            ps.setLong(1, gs.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GeoPkgStyleSheet sheet = mapStylesheet(rs, gs);
                    result.add(sheet);
                }
            }
        }

        return result;
    }

    private GeoPkgStyleSheet mapStylesheet(ResultSet rs, GeoPkgStyle gs) throws SQLException {
        return new GeoPkgStyleSheet(rs.getLong(1), gs, rs.getString(3), rs.getString(4));
    }

    public GeoPkgSymbol getSymbol(String name) throws SQLException {
        String sql =
                format(
                        "SELECT id, symbol, description, uri" + " FROM %s WHERE symbol = ?",
                        SYMBOLS_TABLE);
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql); ) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSymbol(rs);
                }
            }
        }

        return null;
    }

    private GeoPkgSymbol mapSymbol(ResultSet rs) throws SQLException {
        return new GeoPkgSymbol(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4));
    }

    public List<GeoPkgSymbolImage> getImages(GeoPkgSymbol symbol) throws SQLException {
        String sql =
                format(
                        "SELECT sc.id, sc.format, sc.content, sc.uri "
                                + " FROM %s s JOIN %s si ON s.id = si.symbol_id "
                                + " JOIN %s sc ON si.content_id = sc.id "
                                + " WHERE s.symbol = ?",
                        SYMBOLS_TABLE, SYMBOL_IMAGES_TABLE, SYMBOL_CONTENTS_TABLE);
        List<GeoPkgSymbolImage> result = new ArrayList<>();
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql); ) {
            ps.setString(1, symbol.getSymbol());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result.add(mapSymbolImage(rs, symbol));
                }
            }
        }
        return result;
    }

    public void addSymbol(GeoPkgSymbol symbol) throws SQLException {
        String sql =
                String.format(
                        "INSERT INTO %s(symbol, description, uri) VALUES (?, ?, ?)", SYMBOLS_TABLE);
        try (Connection cx = getConnection();
                PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, symbol.getSymbol());
            ps.setString(2, symbol.getDescription());
            ps.setString(3, symbol.getUri());
            ps.executeUpdate();

            symbol.setId(getGeneratedKey(ps));
        }
    }

    public void addImage(GeoPkgSymbolImage image) throws SQLException {
        try (Connection cx = getConnection()) {
            long contentId;

            // add the content
            String contentSql =
                    String.format(
                            "INSERT INTO %s(format, content, uri) VALUES (?, ?, ?)",
                            SYMBOL_CONTENTS_TABLE);
            try (PreparedStatement ps = cx.prepareStatement(contentSql)) {
                ps.setString(1, image.getFormat());
                ps.setBytes(2, image.getContent());
                ps.setString(3, image.getUri());
                ps.executeUpdate();

                contentId = getGeneratedKey(ps);
            }

            // add the image entry (ignore the sprite support for the moment)
            String imageSql =
                    String.format(
                            "INSERT INTO %s(symbol_id, content_id) VALUES (?, ?)",
                            SYMBOL_IMAGES_TABLE);
            try (PreparedStatement ps = cx.prepareStatement(imageSql)) {
                ps.setLong(1, image.getSymbol().getId());
                ps.setLong(2, contentId);
                ps.executeUpdate();
            }
        }
    }

    private GeoPkgSymbolImage mapSymbolImage(ResultSet rs, GeoPkgSymbol symbol)
            throws SQLException {
        return new GeoPkgSymbolImage(
                rs.getLong(1), rs.getString(2), rs.getBytes(3), rs.getString(4), symbol);
    }
}
