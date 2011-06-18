/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.kml;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContext;
import org.geoserver.wms.WMSRequests;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapLayer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Symbolizer;
import org.geotools.xml.transform.Translator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.xml.sax.ContentHandler;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;


/**
 * Tranforms a feature colleciton to a kml "Document" element which contains a
 * "Folder" element consisting of "GroundOverlay" elements.
 * <p>
 * Usage:
 * <pre>
 *  <code>
 *  //have a reference to a map context and output stream
 *  WMSMapContext context = ...
 *  OutputStream output = ...;
 *
 *  KMLRasterTransformer tx = new KMLRasterTransformer( context );
 *  for ( int i = 0; i < context.getLayerCount(); i++ ) {
 *    MapLayer mapLayer = mapConext.getMapLayer( i );
 *
 *    //transform
 *    tx.transform( mapLayer, output );
 *  }
 *  </code>
 * </pre>
 * </p>
 * <p>
 * The inline parameter {@link #setInline(boolean)} controls wether the images
 * for the request are refernces "inline" as local images, or remoteley as wms
 * requests.
 * </p>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class KMLRasterTransformer extends KMLMapTransformer {
    /**
     * Flag controlling wether images are refernces inline or as remote wms calls.
     */
    boolean inline = false;

    public KMLRasterTransformer(WMS wms, WMSMapContext mapContext) {
        super(wms, mapContext, null);

        setNamespaceDeclarationEnabled(false);
    }

    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public Translator createTranslator(ContentHandler handler) {
        return new KMLRasterTranslator(handler);
    }

    class KMLRasterTranslator extends KMLMapTranslatorSupport {
        public KMLRasterTranslator(ContentHandler handler) {
            super(handler);
        }

        public void encode(Object o) throws IllegalArgumentException {
            MapLayer mapLayer = (MapLayer) o;
            int mapLayerOrder = mapContext.indexOf(mapLayer);

            if ( isStandAlone() ) {
                start( "kml" );
            }
            
            //start("Document");
            //element("name", mapLayer.getTitle());

            //start the folder naming it 'layer_<mapLayerOrder>', this is 
            // necessary for a GroundOverlay
            start("Folder");
            element("name", "layer_" + mapLayerOrder);
            element("description", mapLayer.getTitle());

            start("GroundOverlay");
            //element( "name", feature.getID() );
            element("name", mapLayer.getTitle());
            element("drawOrder", Integer.toString(mapLayerOrder));

            //encode the icon
            start("Icon");

            encodeHref(mapLayer);

            element("viewRefreshMode", "never");
            element("viewBoundScale", "0.75");
            end("Icon");

            //encde the bounding box
            ReferencedEnvelope box = new ReferencedEnvelope(mapContext.getAreaOfInterest());
            boolean reprojectBBox = (box.getCoordinateReferenceSystem() != null)
            && !CRS.equalsIgnoreMetadata(box.getCoordinateReferenceSystem(), DefaultGeographicCRS.WGS84); 
            if (reprojectBBox) {
                try {
                    box = box.transform(DefaultGeographicCRS.WGS84, true);
                } catch (Exception e) {
                    throw new ServiceException("Could not transform bbox to WGS84", e,
                            "ReprojectionError", "");
                }
            }
            start("LatLonBox");
            element("north", Double.toString(box.getMaxY()));
            element("south", Double.toString(box.getMinY()));
            element("east", Double.toString(box.getMaxX()));
            element("west", Double.toString(box.getMinX()));
            end("LatLonBox");

            end("GroundOverlay");

            // if the kmplacemark format option is true, add placemarks to the output
            boolean kmplacemark = KMLUtils.getKmplacemark(mapContext.getRequest(), wms);
            if (kmplacemark) {
                SimpleFeatureCollection features = null;
                try {
                    features = KMLUtils
                            .loadFeatureCollection(
                                    (SimpleFeatureSource) mapLayer
                                            .getFeatureSource(), mapLayer,
                                    mapContext, wms, scaleDenominator);
                } catch (Exception ex) {
                    String msg = "Error getting features.";
                    LOGGER.log(Level.WARNING, msg, ex);
                }

                if (features != null && features.size() > 0) {
                    Geometry geom = null;
                    Geometry centroidGeom = null;

                    // get geometry of the area of interest
                    Envelope aoi = mapContext.getAreaOfInterest();
                    GeometryFactory factory = new GeometryFactory();
                    Geometry displayGeom = factory.toGeometry(new Envelope(aoi
                            .getMinX(), aoi.getMaxX(), aoi.getMinY(), aoi
                            .getMaxY()));

                    // get the styles for this feature
                    SimpleFeatureType featureType = features.getSchema();
                    FeatureTypeStyle[] fts = KMLUtils.filterFeatureTypeStyles(
                            mapLayer.getStyle(), featureType);

                    Iterator<SimpleFeature> iter = features.iterator();
                    while (iter.hasNext()) {
                        SimpleFeature ftr = iter.next();
                        geom = (Geometry) ftr.getDefaultGeometry();

                        List<Symbolizer> symbolizers = filterSymbolizers(ftr, fts);
                        if (symbolizers.size() != 0) encodeStyle(ftr, symbolizers);

                        // if this is a multipolygon, get the largest polygon
                        // that intersects the AOI
                        if (geom instanceof MultiPolygon) {
                            double maxSize = -1;
                            int numGeoms = geom.getNumGeometries();
                            for (int i = 0; i < numGeoms; i++) {
                                Polygon poly = (Polygon) geom.getGeometryN(i);
                                if (poly.getArea() > maxSize) {
                                    if (displayGeom.intersects(poly)) {
                                        geom = poly;
                                        maxSize = poly.getArea();
                                    }
                                }
                            }
                        }
                        Geometry g1 = displayGeom.intersection(geom);
                        // skip if the geometry is not in the AOI
                        if (g1.isEmpty())
                            continue;
                        centroidGeom = g1.getCentroid();
                        encodePlacemark(ftr, symbolizers, centroidGeom);
                    }
                }
            }

            end("Folder");

            //end("Document");
            if (isStandAlone()) {
                end( "kml" );
            }
        }

        protected void encodeHref(MapLayer mapLayer) {
            if (inline) {
                //inline means reference the image "inline" as in kmz
                // use the mapLayerOrder
                int mapLayerOrder = mapContext.indexOf(mapLayer);
                element("href", "images/layer_" + mapLayerOrder + ".png");
            } else {
                //reference the image as a remote wms call
                element("href",  
                        WMSRequests.getGetMapUrl(
                            mapContext.getRequest(),
                            mapLayer,
                            0,
                            mapContext.getAreaOfInterest(),
                            new String[]{
                                "format", "image/png",
                                "transparent", "true"
                            }
                            )
                       );
            }
        }
    }
}
