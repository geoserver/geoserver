/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.regionate;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.wms.WMSMapContent;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.api.geometry.BoundingBox;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.projection.ProjectionException;
import org.geotools.util.CanonicalSet;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

/**
 * Base class for regionating strategies. Common functionality provided:
 *
 * <ul>
 *   <li>tiling based on the TMS tiling recommendation
 *   <li>caching the assignment of a feature in a specific tile in an HSQL database stored in the data directory
 *   <li>
 *
 * @author Andrea Aime - OpenGeo
 * @author David Winslow - OpenGeo
 * @author Arne Kepp - OpenGeo
 */
public abstract class CachedHierarchyRegionatingStrategy implements RegionatingStrategy {
    static Logger LOGGER = Logging.getLogger("org.geoserver.geosearch");

    static final double MAX_ERROR = 0.02;

    static final Set<String> NO_FIDS = Collections.emptySet();

    /**
     * This structure is used to make sure that multiple threads end up using the same table name object, so that we can
     * use it as a synchonization token
     */
    static CanonicalSet<String> canonicalizer = CanonicalSet.newInstance(String.class);

    static {
        try {
            // make sure, once and for all, that HSQL is around
            Class.forName("org.hsqldb.jdbcDriver");
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize the class constants", e);
        }
    }

    /** The original area occupied by the data */
    protected ReferencedEnvelope dataEnvelope;

    /** Reference to the layer being regionated */
    protected FeatureTypeInfo featureType;

    /** The max number of features per tile */
    protected Integer featuresPerTile;

    /** The name of the database that will contain the fid to tile cache */
    protected String tableName;

    /** geoserver configuration */
    protected GeoServer gs;

    protected CachedHierarchyRegionatingStrategy(GeoServer gs) {
        this.gs = gs;
    }

    @Override
    public Filter getFilter(WMSMapContent context, Layer layer) {
        Catalog catalog = gs.getCatalog();
        Set<String> featuresInTile = Collections.emptySet();
        try {
            // grab information needed to reach the db and get a hold to a db
            // connection
            FeatureSource featureSource = layer.getFeatureSource();
            featureType = catalog.getFeatureTypeByName(featureSource.getName());

            String dataDir = catalog.getResourceLoader().getBaseDirectory().getCanonicalPath();
            tableName = getDatabaseName(context, layer);

            // grab the features per tile, use a default if user did not
            // provide a decent value. The default should fill up the
            // tile when it shows up.
            featuresPerTile = featureType.getMetadata().get("kml.regionateFeatureLimit", Integer.class);
            if (featuresPerTile == null || featuresPerTile.intValue() <= 1) featuresPerTile = 64;

            // sanity check, the layer is not geometryless
            if (featureType.getFeatureType().getGeometryDescriptor() == null)
                throw new ServiceException(featureType.getName() + " is geometryless, cannot generate KML!");

            // make sure the request is within the data bounds, allowing for a
            // small error
            ReferencedEnvelope requestedEnvelope = context.getRenderingArea().transform(Tile.WGS84, true);
            LOGGER.log(Level.FINE, "Requested tile: {0}", requestedEnvelope);
            dataEnvelope = featureType.getLatLonBoundingBox();

            // decide which tile we need to load/compute, and make sure
            // it's a valid tile request, that is, that is does fit with
            // the general tiling scheme (minus an eventual small error)
            Tile tile = new CachedTile(requestedEnvelope);
            ReferencedEnvelope tileEnvelope = tile.getEnvelope();
            if (!envelopeMatch(tileEnvelope, requestedEnvelope))
                throw new ServiceException("Invalid bounding box request, it does not fit "
                        + "the nearest regionating tile. Requested area: "
                        + requestedEnvelope
                        + ", "
                        + "nearest tile: "
                        + tileEnvelope);

            // oki doki, let's compute the fids in the requested tile
            featuresInTile = getFeaturesForTile(dataDir, tile);
            LOGGER.log(Level.FINE, "Found " + featuresInTile.size() + " features in tile " + tile.toString());
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Error occurred while pre-processing regionated features", t);
            throw new ServiceException("Failure while pre-processing regionated features", t);
        }

        // This okay, just means the tile is empty
        if (featuresInTile.isEmpty()) {
            throw new HttpErrorCodeException(204);
        } else {
            FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
            Set<FeatureId> ids = new HashSet<>();
            for (String fid : featuresInTile) {
                ids.add(ff.featureId(fid));
            }
            return ff.id(ids);
        }
    }

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD") // well spotted, but the db contents are not sensitive
    private static Connection getHsqlConnection(String dbDir, String dbName) throws SQLException {
        return DriverManager.getConnection(
                "jdbc:hsqldb:file:" + dbDir + "/hsqlcache_" + dbName, "geoserver", "geopass");
    }

    @Override
    public void clearCache(FeatureTypeInfo cfg) {
        try {
            GeoServerResourceLoader loader = gs.getCatalog().getResourceLoader();
            Resource geosearch = loader.get("geosearch");
            if (geosearch.getType() == Type.DIRECTORY) {
                clearHsqlDatabase(geosearch.dir(), getDatabaseName(cfg));
            }
        } catch (Exception ioe) {
            LOGGER.severe("Couldn't clear out config dir due to: " + ioe);
        }
    }

    public static void clearAllHsqlDatabases(File dbDir) {
        // find all the databases in the given directory
        List<String> allDbNames = Stream.of(dbDir.listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .filter(fileName -> fileName.matches("^hsqlcache_.*\\.script$"))
                .map(fileName -> fileName.substring(10, fileName.length() - 7)) // take db name in the middle
                .collect(Collectors.toList());
        for (String dbName : allDbNames) {
            clearHsqlDatabase(dbDir, dbName);
        }
    }

    public static void clearHsqlDatabase(File dbDir, String dbName) {
        // shutdown the database
        try (Connection conn = getHsqlConnection(dbDir.getPath(), dbName)) {
            conn.createStatement().execute("SHUTDOWN");
        } catch (SQLException e) {
            LOGGER.severe("Couldn't clear HSQL regionation database: " + dbName + " exception: " + e);
        }
        // manually delete database files
        Stream.of(dbDir.listFiles())
                .filter(file -> file.getName().startsWith("hsqlcache_" + dbName))
                .forEach(file -> file.delete());
    }

    /**
     * Returns true if the two envelope roughly match, that is, they are about the same size and about the same
     * location. The max difference allowed is {@link #MAX_ERROR}, evaluated as a percentage of the width and height of
     * the envelope. The method assumes both envelopes are in the same CRS
     */
    private boolean envelopeMatch(ReferencedEnvelope tileEnvelope, ReferencedEnvelope expectedEnvelope) {
        double widthRatio = Math.abs(1.0 - tileEnvelope.getWidth() / expectedEnvelope.getWidth());
        double heightRatio = Math.abs(1.0 - tileEnvelope.getHeight() / expectedEnvelope.getHeight());
        double xRatio = Math.abs((tileEnvelope.getMinX() - expectedEnvelope.getMinX()) / tileEnvelope.getWidth());
        double yRatio = Math.abs((tileEnvelope.getMinY() - expectedEnvelope.getMinY()) / tileEnvelope.getHeight());
        return widthRatio < MAX_ERROR && heightRatio < MAX_ERROR && xRatio < MAX_ERROR && yRatio < MAX_ERROR;
    }

    /** Open/creates the db and then reads/computes the tile features */
    private Set<String> getFeaturesForTile(String dataDir, Tile tile) throws Exception {

        // build the synchonization token
        canonicalizer.add(tableName);
        tableName = canonicalizer.get(tableName);
        String synchToken = tableName; // tableName is updatable

        // make sure no two thread in parallel can build the same db
        synchronized (synchToken) {
            try (
            // get a hold to the database that contains the cache (this will
            // eventually create the db)
            Connection conn = getHsqlConnection(dataDir + "/geosearch", tableName);
                    // try to create the table, if it's already there this will fail
                    Statement st = conn.createStatement()) {
                st.execute(
                        "CREATE CACHED TABLE IF NOT EXISTS TILECACHE( " //
                                + "x BIGINT, " //
                                + "y BIGINT, " //
                                + "z INT, " //
                                + "fid varchar (64))");
                st.execute("CREATE INDEX IF NOT EXISTS IDX_TILECACHE ON TILECACHE(x, y, z)");
                return readFeaturesForTile(tile, conn);
            }
        }
    }

    /**
     * Reads/computes the tile feature set
     *
     * @param tile the Tile whose features we must find
     * @param conn the HSQL connection
     */
    protected Set<String> readFeaturesForTile(Tile tile, Connection conn) throws Exception {
        // grab the fids and decide whether we have to compute them
        Set<String> fids = readCachedTileFids(tile, conn);
        if (fids != null) {
            return fids;
        } else {
            // build the synchronization token
            String tileKey = tableName + tile.x + "-" + tile.y + "-" + tile.z;
            canonicalizer.add(tileKey);
            tileKey = canonicalizer.get(tileKey);

            synchronized (tileKey) {
                // might have been built while we were waiting
                fids = readCachedTileFids(tile, conn);
                if (fids != null) return fids;

                // still missing, we need to compute them
                fids = computeFids(tile, conn);
                storeFids(tile, fids, conn);

                // optimization, if we did not manage to fill up this tile,
                // the ones below it will be empty -> mark them as such right
                // away
                if (fids.size() < featuresPerTile)
                    for (Tile child : tile.getChildren()) storeFids(child, NO_FIDS, conn);
            }
        }
        return fids;
    }

    /** Store the fids inside */
    private void storeFids(Tile t, Set<String> fids, Connection conn) throws SQLException {
        try {
            // we are going to execute this one many times,
            // let's prepare it so that the db engine does
            // not have to parse it at every call
            String stmt = "INSERT INTO TILECACHE VALUES (" + t.x + ", " + t.y + ", " + t.z + ", ?)";
            try (PreparedStatement ps = conn.prepareStatement(stmt)) {
                if (fids.isEmpty()) {
                    // we just have to mark the tile as empty
                    ps.setString(1, null);
                    ps.execute();
                } else {
                    // store all the fids
                    conn.setAutoCommit(false);
                    for (String fid : fids) {
                        ps.setString(1, fid);
                        ps.execute();
                    }
                    conn.commit();
                }
            }
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /** Computes the fids that will be stored in the specified tile */
    private Set<String> computeFids(Tile tile, Connection conn) throws Exception {
        Tile parent = tile.getParent();
        Set<String> parentFids = getUpwardFids(parent, conn);
        Set<String> currFids = new HashSet<>();
        // grab the features
        FeatureSource fs = featureType.getFeatureSource(null, null);
        GeometryDescriptor geom = fs.getSchema().getGeometryDescriptor();
        CoordinateReferenceSystem nativeCrs = geom.getCoordinateReferenceSystem();

        ReferencedEnvelope nativeTileEnvelope = null;

        if (!CRS.equalsIgnoreMetadata(Tile.WGS84, nativeCrs)) {
            try {
                nativeTileEnvelope = tile.getEnvelope().transform(nativeCrs, true);
            } catch (ProjectionException pe) {
                // the WGS84 envelope of the tile is too big for this project,
                // let's intersect it with the declared lat/lon bounds then
                LOGGER.log(
                        Level.INFO,
                        "Could not reproject the current tile bounds "
                                + tile.getEnvelope()
                                + " to the native SRS, intersecting with "
                                + "the layer declared lat/lon bounds and retrying");

                // let's compare against the declared data bounds then
                ReferencedEnvelope llEnv = featureType.getLatLonBoundingBox();
                Envelope reduced = tile.getEnvelope().intersection(llEnv);
                if (reduced.isNull() || reduced.getWidth() == 0 || reduced.getHeight() == 0) {
                    // no overlap, no party, the tile will be empty
                    return Collections.emptySet();
                }

                // there is some overlap, let's try the reprojection again.
                // if even this fails, the user has evidently setup the
                // geographics bounds improperly
                ReferencedEnvelope refRed =
                        new ReferencedEnvelope(reduced, tile.getEnvelope().getCoordinateReferenceSystem());
                nativeTileEnvelope = refRed.transform(nativeCrs, true);
            }
        } else {
            nativeTileEnvelope = tile.getEnvelope();
        }

        try (FeatureIterator fi = getSortedFeatures(geom, tile.getEnvelope(), nativeTileEnvelope, conn)) {
            // if the crs is not wgs84, we'll need to transform the point
            MathTransform tx = null;
            double[] coords = new double[2];

            // scan counting how many fids we've collected
            boolean first = true;
            while (fi.hasNext() && currFids.size() < featuresPerTile) {
                // grab the feature, skip it if it's already in a parent element
                SimpleFeature f = (SimpleFeature) fi.next();
                if (parentFids.contains(f.getID())) continue;

                // check the need for a transformation
                if (first) {
                    first = false;
                    CoordinateReferenceSystem nativeCRS = f.getType().getCoordinateReferenceSystem();
                    featureType.getFeatureType().getCoordinateReferenceSystem();
                    if (nativeCRS != null && !CRS.equalsIgnoreMetadata(nativeCRS, Tile.WGS84)) {
                        tx = CRS.findMathTransform(nativeCRS, Tile.WGS84);
                    }
                }

                // see if the features is to be included in this tile
                Point p = ((Geometry) f.getDefaultGeometry()).getCentroid();
                coords[0] = p.getX();
                coords[1] = p.getY();
                if (tx != null) tx.transform(coords, 0, coords, 0, 1);
                if (tile.contains(coords[0], coords[1])) currFids.add(f.getID());
            }
        }

        return currFids;
    }

    /**
     * Returns all the features in the specified envelope, sorted according to the priority used for regionating. The
     * features returned do not have to be the feature type ones, it's sufficient that they have the same FID and a
     * geometry whose centroid is the same as the original feature one.
     *
     * @param indexConnection a connection to the feature id cache db
     */
    protected abstract FeatureIterator getSortedFeatures(
            GeometryDescriptor geom,
            ReferencedEnvelope latLongEnvelope,
            ReferencedEnvelope nativeEnvelope,
            Connection indexConnection)
            throws Exception;

    /** Returns a set of all the fids in the specified tile and in the parents of it, recursing up to the root tile */
    private Set<String> getUpwardFids(Tile tile, Connection conn) throws Exception {
        // recursion stop condition
        if (tile == null) {
            return Collections.emptySet();
        }

        // return the curren tile fids, and recurse up to the parent
        Set<String> fids = new HashSet<>();
        fids.addAll(readFeaturesForTile(tile, conn));
        Tile parent = tile.getParent();
        if (parent != null) {
            fids.addAll(getUpwardFids(parent, conn));
        }
        return fids;
    }

    /**
     * Here we have three cases
     *
     * <ul>
     *   <li>the tile was already computed, and it resulted to be empty. We leave a "x,y,z,null" marker to know if that
     *       happened, and in this case the returned set will be empty
     *   <li>the tile was already computed, and we have data, the returned sest will be non empty
     *   <li>the tile is new, the db contains nothing, in this case we return "null"
     *       <ul>
     */
    protected Set<String> readCachedTileFids(Tile tile, Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT fid FROM TILECACHE where x = "
                        + tile.x
                        + " AND y = "
                        + tile.y
                        + " and z = "
                        + tile.z)) {
            // decide whether we have to collect the fids or just to
            // return that the tile was empty
            Set<String> fids = null;
            if (rs.next()) {
                String fid = rs.getString(1);
                if (fid == null) {
                    return Collections.emptySet();
                } else {
                    fids = new HashSet<>();
                    fids.add(fid);
                }
            }
            // fill the set with the collected fids
            while (rs.next()) {
                fids.add(rs.getString(1));
            }
            return fids;
        }
    }

    /** Returns the name to be used for the database. Should be unique for this specific regionated layer. */
    protected String getDatabaseName(WMSMapContent con, Layer layer) throws Exception {
        return getDatabaseName(featureType);
    }

    protected String getDatabaseName(FeatureTypeInfo cfg) throws Exception {
        return cfg.getNamespace().getPrefix() + "_" + cfg.getName();
    }

    /**
     * A regionating tile identified by its coordinates
     *
     * @author Andrea Aime
     */
    protected class CachedTile extends Tile {

        /** Creates a new tile with the given coordinates */
        public CachedTile(long x, long y, long z) {
            super(x, y, z);
        }

        /**
         * Tile containment check is not trivial due to a couple of issues:
         *
         * <ul>
         *   <li>centroids sitting on the tile borders must be associated to exactly one tile, so we have to consider
         *       only two borders as inclusive in general (S and W) but add on occasion the other two when we reach the
         *       extent of our data set
         *   <li>coordinates going beyond the natural lat/lon range
         * </ul>
         *
         * This code takes care of the first, whilst the second issue remains as a TODO
         */
        @Override
        public boolean contains(double x, double y) {
            if (super.contains(x, y)) {
                return true;
            }

            // else check if we are on a border tile and the point
            // happens to sit right on the border we usually don't include
            double maxx = envelope.getMaxX();
            double maxy = envelope.getMaxY();
            if (x == maxx && x >= dataEnvelope.getMaxX()) {
                return true;
            }
            if (y == maxy && y >= dataEnvelope.getMaxY()) {
                return true;
            }
            return false;
        }

        /** Builds the best matching tile for the specified envelope */
        public CachedTile(ReferencedEnvelope wgs84Envelope) {
            super(wgs84Envelope);
        }

        public CachedTile(Tile parent) {
            super(parent.x, parent.y, parent.z);
        }

        /** Returns the parent of this tile, or null if this tile is (one of) the root of the current dataset */
        @Override
        public Tile getParent() {
            if (envelope.contains((BoundingBox) dataEnvelope)) {
                return null;
            } else {
                Tile parent = super.getParent();
                if (parent != null) {
                    // wrap it, as we have some custom logic working against the data envelope here
                    parent = new CachedTile(super.getParent());
                }

                return parent;
            }
        }
    }
}
