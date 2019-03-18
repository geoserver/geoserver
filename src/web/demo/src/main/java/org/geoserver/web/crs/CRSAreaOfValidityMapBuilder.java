/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.crs;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.geotools.util.factory.GeoTools;
import org.geotools.xml.styling.SLDParser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Helper class to dynamically create a graphic representation of the area of validity for a {@link
 * CoordinateReferenceSystem coordinate reference system}.
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
class CRSAreaOfValidityMapBuilder {

    private static final int DEFAULT_MAP_WIDTH = 400;

    private static final int DEFAULT_MAP_HEIGHT = 200;

    private static final GeometryFactory gf = new GeometryFactory();

    private final int mapWidth;

    private final int mapHeight;

    private static Map<String, Style> STYLES = new WeakHashMap<String, Style>();

    private static WeakReference<DataStore> LATLON = null;

    public CRSAreaOfValidityMapBuilder() {
        this(DEFAULT_MAP_WIDTH, DEFAULT_MAP_HEIGHT);
    }

    public CRSAreaOfValidityMapBuilder(int mapWidth, int mapHeight) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
    }

    @SuppressWarnings("unchecked")
    private SimpleFeatureSource getFeatureSource(final URL shpfile) throws IOException {
        Map params = new HashMap<String, String>();
        params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, "false");
        params.put(ShapefileDataStoreFactory.URLP.key, shpfile);
        DataStore ds = DataStoreFinder.getDataStore(params);
        return ds.getFeatureSource(ds.getTypeNames()[0]);
    }

    private Geometry getGeographicBoundingBox(CoordinateReferenceSystem crs) {
        GeographicBoundingBox envelope = CRS.getGeographicBoundingBox(crs);
        if (envelope == null) {
            return null;
        }

        final double westBoundLongitude = envelope.getWestBoundLongitude();
        final double eastBoundLongitude = envelope.getEastBoundLongitude();
        final double southBoundLatitude = envelope.getSouthBoundLatitude();
        final double northBoundLatitude = envelope.getNorthBoundLatitude();

        final int numSteps = 80;
        Geometry geogBoundingGeom;

        if (westBoundLongitude < eastBoundLongitude) {
            geogBoundingGeom =
                    createBoundingPolygon(
                            westBoundLongitude,
                            eastBoundLongitude,
                            southBoundLatitude,
                            northBoundLatitude,
                            numSteps);
        } else {
            // the geographic bounds cross the day line (lon -180/180), trick it into two adjacent
            // polygons
            Polygon eastPolygon =
                    createBoundingPolygon(
                            -180,
                            eastBoundLongitude,
                            southBoundLatitude,
                            northBoundLatitude,
                            numSteps);

            Polygon westPolygon =
                    createBoundingPolygon(
                            westBoundLongitude,
                            180,
                            southBoundLatitude,
                            northBoundLatitude,
                            numSteps);

            geogBoundingGeom = gf.createMultiPolygon(new Polygon[] {eastPolygon, westPolygon});
        }
        return geogBoundingGeom;
    }

    private Polygon createBoundingPolygon(
            final double westBoundLongitude,
            final double eastBoundLongitude,
            final double southBoundLatitude,
            final double northBoundLatitude,
            final int numSteps) {
        // build a densified LinearRing so it does reproject better
        final double dx = (eastBoundLongitude - westBoundLongitude) / numSteps;
        final double dy = (northBoundLatitude - southBoundLatitude) / numSteps;

        List<Coordinate> coords = new ArrayList<Coordinate>(4 * numSteps + 1);

        double x = westBoundLongitude;
        for (int i = 0; i < numSteps; i++) {
            coords.add(new Coordinate(x, southBoundLatitude));
            x += dx;
        }
        double y = southBoundLatitude;
        for (int i = 0; i < numSteps; i++) {
            coords.add(new Coordinate(eastBoundLongitude, y));
            y += dy;
        }
        x = eastBoundLongitude;
        for (int i = 0; i < numSteps; i++) {
            coords.add(new Coordinate(x, northBoundLatitude));
            x -= dx;
        }
        y = northBoundLatitude;
        for (int i = 0; i < numSteps; i++) {
            coords.add(new Coordinate(westBoundLongitude, y));
            y -= dy;
        }
        coords.add(new Coordinate(westBoundLongitude, southBoundLatitude));

        Coordinate[] coordinates = coords.toArray(new Coordinate[coords.size()]);
        LinearRing shell = gf.createLinearRing(coordinates);
        Polygon polygon = gf.createPolygon(shell, null);
        return polygon;
    }

    private Style getStyle(final String styleName) {
        Style style = STYLES.get(styleName);
        if (style == null) {
            StyleFactory styleFactory =
                    CommonFactoryFinder.getStyleFactory(GeoTools.getDefaultHints());
            SLDParser parser = new SLDParser(styleFactory);
            try {
                parser.setInput(getClass().getResource(styleName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            StyledLayerDescriptor sld = parser.parseSLD();
            UserLayer layer = (UserLayer) sld.getStyledLayers()[0];
            style = layer.getUserStyles()[0];
            STYLES.put(styleName, style);
        }
        return style;
    }

    public RenderedImage createMapFor(
            CoordinateReferenceSystem crs, org.locationtech.jts.geom.Envelope areaOfInterest)
            throws IOException {
        BufferedImage image = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        createMapFor(crs, areaOfInterest, graphics);
        graphics.dispose();
        return image;
    }

    @SuppressWarnings("unchecked")
    public void createMapFor(
            final CoordinateReferenceSystem crs,
            final org.locationtech.jts.geom.Envelope areaOfInterest,
            final Graphics2D graphics)
            throws IOException {

        Geometry geographicBoundingBox = getGeographicBoundingBox(crs);
        MapContent mapContent = getMapContext(crs, geographicBoundingBox, areaOfInterest);

        graphics.setColor(new Color(153, 179, 204));
        graphics.fillRect(0, 0, mapWidth, mapHeight);

        Rectangle paintArea = new Rectangle(mapWidth, mapHeight);

        mapContent.getViewport().setBounds(new ReferencedEnvelope(areaOfInterest, crs));

        GTRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(mapContent);
        RenderingHints hints =
                new RenderingHints(
                        RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        renderer.setJava2DHints(hints);

        Map renderingHints = new HashMap();
        renderingHints.put("optimizedDataLoadingEnabled", Boolean.TRUE);
        renderingHints.put(StreamingRenderer.ADVANCED_PROJECTION_HANDLING_KEY, Boolean.TRUE);
        renderingHints.put(StreamingRenderer.CONTINUOUS_MAP_WRAPPING, Boolean.TRUE);
        renderer.setRendererHints(renderingHints);
        renderer.paint(graphics, paintArea, areaOfInterest);

        mapContent.dispose();
    }

    private SimpleFeature createCrsBoundsFeature(Geometry geom, CoordinateReferenceSystem crs) {
        SimpleFeatureType featureType;

        SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
        sftb.setName("Bounds");
        try {
            sftb.add("the_geom", Geometry.class, CRS.decode("EPSG:4326", true));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        featureType = sftb.buildFeatureType();

        SimpleFeature feature = SimpleFeatureBuilder.template(featureType, null);
        feature.setAttribute("the_geom", geom);
        return feature;
    }

    private Layer createCrsLayer(Geometry geom, CoordinateReferenceSystem crs) {
        DefaultFeatureCollection collection = new DefaultFeatureCollection();
        collection.add(createCrsBoundsFeature(geom, crs));

        Style style = getStyle("crs.sld");

        FeatureLayer ml = new FeatureLayer(collection, style);
        return ml;
    }

    private MapContent getMapContext(
            CoordinateReferenceSystem crs,
            Geometry geographicBoundingBox,
            org.locationtech.jts.geom.Envelope areaOfInterest)
            throws IOException {

        MapContent mapContent = new MapContent();

        Style style;
        URL shpfile;
        SimpleFeatureSource source;

        shpfile = getClass().getResource("TM_WORLD_BORDERS.shp");
        source = getFeatureSource(shpfile);
        style = getStyle("TM_WORLD_BORDERS.sld");
        mapContent.addLayer(new FeatureLayer(source, style));

        source = getLatLonFeatureSource();
        style = getStyle("latlon.sld");
        mapContent.addLayer(new FeatureLayer(source, style));

        shpfile = getClass().getResource("cities.shp");
        source = getFeatureSource(shpfile);
        style = getStyle("cities.sld");
        mapContent.addLayer(new FeatureLayer(source, style));

        Layer layer = createCrsLayer(geographicBoundingBox, crs);
        mapContent.addLayer(layer);

        return mapContent;
    }

    private SimpleFeatureSource getLatLonFeatureSource() {
        try {
            DataStore ds = LATLON == null ? null : LATLON.get();
            if (ds == null) {
                ds = createLatLonDataStore();
                LATLON = new WeakReference<DataStore>(ds);
            }
            return ds.getFeatureSource("latlon");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DataStore createLatLonDataStore() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("latlon");
        tb.add("the_geom", LineString.class, CRS.decode("EPSG:4326", true));
        tb.add("level", Integer.class);

        SimpleFeatureType type = tb.buildFeatureType();
        MemoryDataStore ds = new MemoryDataStore();

        ds.createSchema(type);

        FeatureWriter<SimpleFeatureType, SimpleFeature> writer;
        writer = ds.getFeatureWriterAppend("latlon", Transaction.AUTO_COMMIT);

        for (int lon = -180; lon < 180; lon += 5) {
            for (int lat = -90; lat < 90; lat += 5) {
                LineString geom;
                int level;

                geom =
                        gf.createLineString(
                                new Coordinate[] {
                                    new Coordinate(lon, lat), new Coordinate(lon, lat + 5)
                                });

                level = 1;
                if (lon % 10 == 0) {
                    level = 10;
                }
                if (lon % 30 == 0) {
                    level = 30;
                }

                SimpleFeature f;
                f = writer.next();
                f.setAttribute(0, geom);
                f.setAttribute(1, Integer.valueOf(level));
                writer.write();

                geom =
                        gf.createLineString(
                                new Coordinate[] {
                                    new Coordinate(lon, lat), new Coordinate(lon + 5, lat)
                                });

                level = 1;
                if (lat % 10 == 0) {
                    level = 10;
                }
                if (lat % 30 == 0) {
                    level = 30;
                }
                f = writer.next();
                f.setAttribute(0, geom);
                f.setAttribute(1, Integer.valueOf(level));
                writer.write();
            }
        }
        writer.close();

        return ds;
    }
}
