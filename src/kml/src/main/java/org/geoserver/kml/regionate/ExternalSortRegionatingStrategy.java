/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.regionate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMapContent;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.data.jdbc.JDBCUtils;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class ExternalSortRegionatingStrategy extends CachedHierarchyRegionatingStrategy {

    /** The feature type for the features that we'll return back from the index */
    static final SimpleFeatureType IDX_FEATURE_TYPE;

    /** Java type to HSQL type map (covers only types that do not have a size) */
    static Map<Class<?>, String> CLASS_MAPPINGS = new LinkedHashMap<>();

    static {
        CLASS_MAPPINGS.put(Boolean.class, "BOOLEAN");
        CLASS_MAPPINGS.put(Byte.class, "TINYINT");
        CLASS_MAPPINGS.put(Short.class, "SMALLINT");
        CLASS_MAPPINGS.put(Character.class, "CHAR");
        CLASS_MAPPINGS.put(Integer.class, "INT");
        CLASS_MAPPINGS.put(Long.class, "BIGINT");
        CLASS_MAPPINGS.put(BigInteger.class, "BIGINT");
        CLASS_MAPPINGS.put(BigDecimal.class, "DECIMAL");
        CLASS_MAPPINGS.put(Float.class, "REAL");
        CLASS_MAPPINGS.put(Double.class, "DOUBLE");
        CLASS_MAPPINGS.put(java.util.Date.class, "DATE");
        CLASS_MAPPINGS.put(java.sql.Date.class, "DATE");
        CLASS_MAPPINGS.put(java.sql.Time.class, "TIME");
        CLASS_MAPPINGS.put(java.sql.Timestamp.class, "TIMESTAMP");

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.crs(Tile.WGS84);
        tb.add("point", Point.class);
        tb.setName("FeatureCentroids");
        IDX_FEATURE_TYPE = tb.buildFeatureType();
    }

    String attribute;

    FeatureSource fs;

    String hsqlType;

    public ExternalSortRegionatingStrategy(GeoServer gs) {
        super(gs);
    }

    @Override
    protected final String getDatabaseName(WMSMapContent con, Layer layer) throws Exception {
        fs = layer.getFeatureSource();
        SimpleFeatureType ft = (SimpleFeatureType) fs.getSchema();

        checkAttribute(con, ft);

        // make sure a special db for this layer and attribute will be created
        return super.getDatabaseName(con, layer) + "_" + attribute;
    }

    @Override
    protected final String getDatabaseName(FeatureTypeInfo cfg) throws Exception {
        return super.getDatabaseName(cfg) + "_" + checkAttribute(cfg);
    }

    protected void checkAttribute(WMSMapContent con, SimpleFeatureType ft) {
        // find out which attribute we're going to use
        Map options = con.getRequest().getFormatOptions();
        attribute = (String) options.get("regionateAttr");
        if (attribute == null) attribute = checkAttribute(featureType);
        if (attribute == null)
            throw new ServiceException("Regionating attribute has not been specified");

        // Make sure the attribute is actually there
        AttributeDescriptor ad = ft.getDescriptor(attribute);
        if (ad == null) {
            throw new ServiceException(
                    "Could not find regionating attribute "
                            + attribute
                            + " in layer "
                            + featureType.getName());
        }

        // Make sure we know how to turn that attribute into a HSQL type
        hsqlType = getHSQLDataType(ad);
        if (hsqlType == null)
            throw new ServiceException(
                    "Attribute type "
                            + ad.getType()
                            + " is not "
                            + "supported for external sorting on "
                            + featureType.getName()
                            + "#"
                            + attribute);
    }

    protected String checkAttribute(FeatureTypeInfo cfg) {
        return MapLayerInfo.getRegionateAttribute(cfg);
    }

    @Override
    public FeatureIterator getSortedFeatures(
            GeometryDescriptor geom,
            ReferencedEnvelope latLongEnvelope,
            ReferencedEnvelope nativeEnvelope,
            Connection cacheConn)
            throws Exception {
        // first of all, let's check if the geometry index table is there
        try (Statement st = cacheConn.createStatement()) {
            try {
                st.executeQuery("SELECT * FROM FEATUREIDX LIMIT 1");
            } catch (SQLException e) {
                buildIndex(cacheConn);
            }
        }

        return new IndexFeatureIterator(cacheConn, latLongEnvelope);
    }

    protected String getHSQLDataType(AttributeDescriptor ad) {
        if (String.class.equals(ad.getType().getBinding())) {
            int length = FeatureTypes.getFieldLength(ad);
            if (length <= 0) length = 255;
            return "VARCHAR(" + length + ")";
        } else {
            return CLASS_MAPPINGS.get(ad.getType().getBinding());
        }
    }

    void buildIndex(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute(
                    "CREATE CACHED TABLE FEATUREIDX(" //
                            + "X DOUBLE, " //
                            + "Y DOUBLE, " //
                            + "FID VARCHAR(64), " //
                            + "ORDER_FIELD "
                            + hsqlType
                            + ")");
            st.execute("CREATE INDEX FEATUREIDX_COORDS ON FEATUREIDX(X, Y)");
            st.execute("CREATE INDEX FEATUREIDX_ORDER_FIELD ON FEATUREIDX(ORDER_FIELD)");
        }

        // prepare this statement so that the sql parser has to deal
        // with it just once
        try (PreparedStatement ps =
                conn.prepareStatement(
                        "INSERT INTO "
                                + "FEATUREIDX(X, Y, FID, ORDER_FIELD) VALUES (?, ?, ?, ?)")) {

            // build an optimized query, loading only the necessary attributes
            GeometryDescriptor geom = fs.getSchema().getGeometryDescriptor();
            CoordinateReferenceSystem nativeCrs = geom.getCoordinateReferenceSystem();
            Query q = new Query();

            if (geom.getLocalName().equals(attribute)) {
                q.setPropertyNames(geom.getLocalName());
            } else {
                q.setPropertyNames(attribute, geom.getLocalName());
            }

            // setup the eventual transform
            MathTransform tx = null;
            double[] coords = new double[2];
            if (!CRS.equalsIgnoreMetadata(nativeCrs, Tile.WGS84))
                tx = CRS.findMathTransform(nativeCrs, Tile.WGS84, true);

            // read all the features and fill the index table
            // make it so the insertion is a single big transaction, should
            // be faster,
            // provided it does not kill HSQL...
            conn.setAutoCommit(false);
            try (FeatureIterator fi = fs.getFeatures(q).features()) {
                while (fi.hasNext()) {
                    // grab the centroid and transform it in 4326 if necessary
                    SimpleFeature f = (SimpleFeature) fi.next();
                    Geometry g = (Geometry) f.getDefaultGeometry();
                    if (g.isEmpty()) {
                        continue;
                    }
                    Point centroid = g.getCentroid();

                    // robustness check for bad geometries
                    if (Double.isNaN(centroid.getX()) || Double.isNaN(centroid.getY())) {
                        LOGGER.warning(
                                "Could not calculate centroid for feature "
                                        + f.getID()
                                        + "; g =  "
                                        + g.toText());
                        continue;
                    }

                    coords[0] = centroid.getX();
                    coords[1] = centroid.getY();
                    if (tx != null) tx.transform(coords, 0, coords, 0, 1);

                    // insert
                    ps.setDouble(1, coords[0]);
                    ps.setDouble(2, coords[1]);
                    ps.setString(3, f.getID());
                    ps.setObject(4, getSortAttributeValue(f));
                    ps.execute();
                }
            }
            // todo: commit every 1000 features or so. No transaction is
            // slower, but too big transaction imposes a big overhead on the db
            conn.commit();

            // hum, shall we kick HSQL so that it updates the statistics?
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /** Returns the value that will be inserted into the HSQL index as the sorting field */
    protected Object getSortAttributeValue(SimpleFeature f) {
        return f.getAttribute(attribute);
    }

    public static class IndexFeatureIterator implements FeatureIterator {
        SimpleFeatureBuilder builder;

        GeometryFactory gf;

        Statement st;

        ResultSet rs;

        boolean nextCalled;

        boolean next;

        public IndexFeatureIterator(Connection cacheConn, ReferencedEnvelope envelope)
                throws Exception {
            // grab all of the geometries sitting inside the envelope

            try {
                st = cacheConn.createStatement();
                // we are using Math.nextDown and Math.nextUp methods to prevent double precision
                // problems
                String sql =
                        "SELECT X, Y, FID \n"
                                + "FROM FEATUREIDX\n" //
                                + "WHERE X > "
                                + Math.nextDown(envelope.getMinX())
                                + "\n"
                                + "AND X < "
                                + Math.nextUp(envelope.getMaxX())
                                + "\n"
                                + "AND Y > "
                                + Math.nextDown(envelope.getMinY())
                                + "\n"
                                + "AND Y < "
                                + Math.nextUp(envelope.getMaxY())
                                + "\n"
                                + "ORDER BY ORDER_FIELD DESC";
                rs = st.executeQuery(sql);
                // make sure everything is properly closed in case of
                // exception
            } catch (SQLException e) {
                close();
            }

            // prepare the builders we'll use to create all of the features
            builder = new SimpleFeatureBuilder(IDX_FEATURE_TYPE);
            gf = new GeometryFactory();
        }

        @Override
        public void close() {
            JDBCUtils.close(rs);
            JDBCUtils.close(st);
        }

        @Override
        public boolean hasNext() {
            // the contract of the iterator does not say this will be
            // called just once, we have to guard against multiple calls
            if (!nextCalled)
                try {
                    next = rs.next();
                    nextCalled = true;
                } catch (SQLException e) {
                    close();
                    throw new RuntimeException("Error while accessing next db record", e);
                }

            return next;
        }

        @Override
        public Feature next() throws NoSuchElementException {
            if (!nextCalled) hasNext();
            nextCalled = false;
            try {
                double x = rs.getDouble(1);
                double y = rs.getDouble(2);
                builder.add(gf.createPoint(new Coordinate(x, y)));
                return builder.buildFeature(rs.getString(3));
            } catch (SQLException e) {
                close();
                throw new RuntimeException("Problems reading the geometry index");
            }
        }
    }
}
