/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.test;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;

/**
 * Abstract TestData class that defines names for all the layers from the WFS, WMS, and WCS CITE
 * tests.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class CiteTestData implements TestData {

    // //// WMS 1.1.1
    /** WMS 1.1.1 cite namespace + uri */
    public static String CITE_PREFIX = "cite";

    public static String CITE_URI = "http://www.opengis.net/cite";

    /** featuretype name for WMS 1.1.1 CITE BasicPolygons features */
    public static QName BASIC_POLYGONS = new QName(CITE_URI, "BasicPolygons", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Bridges features */
    public static QName BRIDGES = new QName(CITE_URI, "Bridges", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Buildings features */
    public static QName BUILDINGS = new QName(CITE_URI, "Buildings", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Divided Routes features */
    public static QName DIVIDED_ROUTES = new QName(CITE_URI, "DividedRoutes", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Forests features */
    public static QName FORESTS = new QName(CITE_URI, "Forests", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Lakes features */
    public static QName LAKES = new QName(CITE_URI, "Lakes", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Map Neatliine features */
    public static QName MAP_NEATLINE = new QName(CITE_URI, "MapNeatline", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Named Places features */
    public static QName NAMED_PLACES = new QName(CITE_URI, "NamedPlaces", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Ponds features */
    public static QName PONDS = new QName(CITE_URI, "Ponds", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Road Segments features */
    public static QName ROAD_SEGMENTS = new QName(CITE_URI, "RoadSegments", CITE_PREFIX);

    /** featuretype name for WMS 1.1.1 CITE Streams features */
    public static QName STREAMS = new QName(CITE_URI, "Streams", CITE_PREFIX);

    // /// WFS 1.0
    /** WFS 1.0 cdf namespace + uri */
    public static String CDF_PREFIX = "cdf";

    public static String CDF_URI = "http://www.opengis.net/cite/data";

    /** featuretype name for WFS 1.0 CITE Deletes features */
    public static QName DELETES = new QName(CDF_URI, "Deletes", CDF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Fifteen features */
    public static QName FIFTEEN = new QName(CDF_URI, "Fifteen", CDF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Inserts features */
    public static QName INSERTS = new QName(CDF_URI, "Inserts", CDF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Inserts features */
    public static QName LOCKS = new QName(CDF_URI, "Locks", CDF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Nulls features */
    public static QName NULLS = new QName(CDF_URI, "Nulls", CDF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Other features */
    public static QName OTHER = new QName(CDF_URI, "Other", CDF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Nulls features */
    public static QName SEVEN = new QName(CDF_URI, "Seven", CDF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Updates features */
    public static QName UPDATES = new QName(CDF_URI, "Updates", CDF_PREFIX);

    /** cgf namespace + uri */
    public static String CGF_PREFIX = "cgf";

    public static String CGF_URI = "http://www.opengis.net/cite/geometry";

    /** featuretype name for WFS 1.0 CITE Lines features */
    public static QName LINES = new QName(CGF_URI, "Lines", CGF_PREFIX);

    /** featuretype name for WFS 1.0 CITE MLines features */
    public static QName MLINES = new QName(CGF_URI, "MLines", CGF_PREFIX);

    /** featuretype name for WFS 1.0 CITE MPoints features */
    public static QName MPOINTS = new QName(CGF_URI, "MPoints", CGF_PREFIX);

    /** featuretype name for WFS 1.0 CITE MPolygons features */
    public static QName MPOLYGONS = new QName(CGF_URI, "MPolygons", CGF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Points features */
    public static QName POINTS = new QName(CGF_URI, "Points", CGF_PREFIX);

    /** featuretype name for WFS 1.0 CITE Polygons features */
    public static QName POLYGONS = new QName(CGF_URI, "Polygons", CGF_PREFIX);

    // //// WFS 1.1
    /** sf namespace + uri */
    public static String SF_PREFIX = "sf";

    public static String SF_URI = "http://cite.opengeospatial.org/gmlsf";
    public static QName PRIMITIVEGEOFEATURE = new QName(SF_URI, "PrimitiveGeoFeature", SF_PREFIX);
    public static QName AGGREGATEGEOFEATURE = new QName(SF_URI, "AggregateGeoFeature", SF_PREFIX);
    public static QName GENERICENTITY = new QName(SF_URI, "GenericEntity", SF_PREFIX);

    // WCS 1.0
    public static QName GTOPO_DEM = new QName(CDF_URI, "W020N90", CDF_PREFIX);
    public static QName USA_WORLDIMG = new QName(CDF_URI, "usa", CDF_PREFIX);
    public static String DEM = "dem";
    public static String PNG = "png";
    // WCS 1.1
    public static String WCS_PREFIX = "wcs";
    public static String WCS_URI = "http://www.opengis.net/wcs/1.1.1";
    public static QName TASMANIA_DEM = new QName(WCS_URI, "DEM", WCS_PREFIX);
    public static QName TASMANIA_BM = new QName(WCS_URI, "BlueMarble", WCS_PREFIX);
    public static QName ROTATED_CAD = new QName(WCS_URI, "RotatedCad", WCS_PREFIX);
    public static QName WORLD = new QName(WCS_URI, "World", WCS_PREFIX);
    public static String TIFF = "tiff";

    // DEFAULT
    public static String DEFAULT_PREFIX = "gs";
    public static String DEFAULT_URI = "http://geoserver.org";

    // public static QName ENTIT\u00C9G\u00C9N\u00C9RIQUE = new QName( SF_URI,
    // "Entit\u00E9G\u00E9n\u00E9rique", SF_PREFIX );

    // Extra types
    public static QName GEOMETRYLESS = new QName(CITE_URI, "Geometryless", CITE_PREFIX);

    /** List of all cite types names */
    public static QName[] TYPENAMES =
            new QName[] {
                // WMS 1.1.1
                BASIC_POLYGONS,
                BRIDGES,
                BUILDINGS,
                DIVIDED_ROUTES,
                FORESTS,
                LAKES,
                MAP_NEATLINE,
                NAMED_PLACES,
                PONDS,
                ROAD_SEGMENTS,
                STREAMS,
                GEOMETRYLESS,
                // WFS 1.0
                DELETES,
                FIFTEEN,
                INSERTS,
                LOCKS,
                NULLS,
                OTHER,
                SEVEN,
                UPDATES,
                LINES,
                MLINES,
                MPOINTS,
                MPOLYGONS,
                POINTS,
                POLYGONS,
                // WFS 1.1
                PRIMITIVEGEOFEATURE,
                AGGREGATEGEOFEATURE,
                GENERICENTITY /* ENTIT\u00C9G\u00C9N\u00C9RIQUE */
            };

    /** List of wms type names. */
    public static QName[] WMS_TYPENAMES =
            new QName[] {
                BASIC_POLYGONS,
                BRIDGES,
                BUILDINGS,
                DIVIDED_ROUTES,
                FORESTS,
                LAKES,
                MAP_NEATLINE,
                NAMED_PLACES,
                PONDS,
                ROAD_SEGMENTS,
                STREAMS,
                GEOMETRYLESS
            };

    /** List of wcs type names. */
    public static QName[] WCS_TYPENAMES =
            new QName[] {TASMANIA_DEM, TASMANIA_BM, ROTATED_CAD, WORLD};

    /** List of wfs 1.0 type names. */
    public static QName[] WFS10_TYPENAMES =
            new QName[] {
                DELETES, FIFTEEN, INSERTS, LOCKS, NULLS, OTHER, SEVEN, UPDATES, LINES, MLINES,
                MPOINTS, MPOLYGONS, POINTS, POLYGONS
            };

    /** List of wfs 1.1 type names. */
    public static QName[] WFS11_TYPENAMES =
            new QName[] {
                PRIMITIVEGEOFEATURE,
                AGGREGATEGEOFEATURE,
                GENERICENTITY /* ENTIT\u00C9G\u00C9N\u00C9RIQUE */
            };

    public static QName[] CDF_TYPENAMES =
            new QName[] {DELETES, FIFTEEN, INSERTS, LOCKS, NULLS, OTHER, SEVEN, UPDATES};

    public static QName[] CGF_TYPENAMES =
            new QName[] {LINES, MLINES, MPOINTS, MPOLYGONS, POINTS, POLYGONS};

    public static QName[] SF_TYPENAMES = WFS11_TYPENAMES;

    public static QName[] CITE_TYPENAMES = WMS_TYPENAMES;

    /** map of qname to srs */
    public static HashMap<QName, Integer> SRS = new HashMap<QName, Integer>();

    static {
        for (int i = 0; i < WFS10_TYPENAMES.length; i++) {
            SRS.put(WFS10_TYPENAMES[i], 32615);
        }
        for (int i = 0; i < WFS11_TYPENAMES.length; i++) {
            SRS.put(WFS11_TYPENAMES[i], 4326);
        }
    }

    public static String DEFAULT_VECTOR_STYLE = "Default";
    public static String DEFAULT_RASTER_STYLE = "raster";

    /** map of coverage qname to filename and format */
    public static HashMap<QName, String[]> COVERAGES = new HashMap<QName, String[]>();

    static {
        COVERAGES.put(TASMANIA_DEM, new String[] {"tazdem.tiff", TIFF});
        COVERAGES.put(TASMANIA_BM, new String[] {"tazbm.tiff", TIFF});
        COVERAGES.put(ROTATED_CAD, new String[] {"rotated.tiff", TIFF});
        COVERAGES.put(WORLD, new String[] {"world.tiff", TIFF});
    }

    /** Default lon/lat envelope */
    public static final ReferencedEnvelope DEFAULT_LATLON_ENVELOPE =
            new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84);

    /** Populates a map with prefix to namespace uri mappings for all the mock data namespaces. */
    public static void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put(CITE_PREFIX, CITE_URI);
        namespaces.put(CDF_PREFIX, CDF_URI);
        namespaces.put(CGF_PREFIX, CGF_URI);
        namespaces.put(SF_PREFIX, SF_URI);
    }
}
