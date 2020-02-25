/* (c) 2014-2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.iterator;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.GroundOverlay;
import de.micromata.opengis.kml.v_2_2_0.Icon;
import de.micromata.opengis.kml.v_2_2_0.LatLonBox;
import de.micromata.opengis.kml.v_2_2_0.ViewRefreshMode;
import java.util.Iterator;
import java.util.List;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSRequests;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;

/**
 * Creates an iterator of folders mapping the layers in the map content, using either kml dumps or
 * ground overlays (the classic approach, that is)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class PlainFolderIteratorFactory extends AbstractFolderIteratorFactory {

    public PlainFolderIteratorFactory(KmlEncodingContext context) {
        super(context);
    }

    @Override
    public Iterator<Feature> newIterator() {
        return new PlainFolderGenerator();
    }

    public class PlainFolderGenerator extends AbstractFolderGenerator {

        protected void encodeFolderContents(Layer layer, Folder folder) {
            // now encode the contents (dynamic bit, it may use the Iterator construct)
            if (layer instanceof FeatureLayer) {
                // do we use a KML placemark dump, or a ground overlay?
                if (useVectorOutput(context)) {
                    List<Feature> features =
                            new IteratorList<Feature>(
                                    new FeatureIteratorFactory(context, (FeatureLayer) layer));
                    context.addFeatures(folder, features);
                } else {
                    addGroundOverlay(folder, layer);
                    // in case of ground overlays we might still want to output placemarks
                    // for the
                    if (context.isPlacemarkForced()) {
                        addFeatureCentroids(layer, folder);
                    }
                }
            } else {
                addGroundOverlay(folder, layer);
            }
        }

        /**
         * Adds the feature centroids to the output features, without actually adding the full
         * geometry (used when doing raster overlays of vector data with a desire to retain the
         * popups)
         */
        private void addFeatureCentroids(Layer layer, Folder folder) {
            SimpleFeatureCollection centroids =
                    new KMLCentroidFeatureCollection(
                            context.getCurrentFeatureCollection(), context);
            context.setCurrentFeatureCollection(centroids);
            FeatureLayer centroidsLayer =
                    new FeatureLayer(centroids, layer.getStyle(), layer.getTitle());
            List<Feature> features =
                    new IteratorList<Feature>(new FeatureIteratorFactory(context, centroidsLayer));
            context.addFeatures(folder, features);
        }

        /** Encodes the ground overlay for the specified layer */
        private void addGroundOverlay(Folder folder, Layer layer) {
            int mapLayerOrder = context.getMapContent().layers().indexOf(layer);

            GroundOverlay go = folder.createAndAddGroundOverlay();
            go.setName(layer.getTitle());
            go.setDrawOrder(mapLayerOrder);
            Icon icon = go.createAndSetIcon();
            icon.setHref(getGroundOverlayHRef(layer));
            icon.setViewRefreshMode(ViewRefreshMode.NEVER);
            icon.setViewBoundScale(0.75);

            ReferencedEnvelope box =
                    new ReferencedEnvelope(context.getMapContent().getRenderingArea());
            boolean reprojectBBox =
                    (box.getCoordinateReferenceSystem() != null)
                            && !CRS.equalsIgnoreMetadata(
                                    box.getCoordinateReferenceSystem(), DefaultGeographicCRS.WGS84);
            if (reprojectBBox) {
                try {
                    box = box.transform(DefaultGeographicCRS.WGS84, true);
                } catch (Exception e) {
                    throw new ServiceException(
                            "Could not transform bbox to WGS84", e, "ReprojectionError", "");
                }
            }

            LatLonBox gobox = go.createAndSetLatLonBox();
            gobox.setEast(box.getMaxX());
            gobox.setWest(box.getMinX());
            gobox.setNorth(box.getMaxY());
            gobox.setSouth(box.getMinY());
        }

        String getGroundOverlayHRef(Layer layer) {
            WMSMapContent mapContent = context.getMapContent();
            if (context.isKmz()) {
                // embed the ground overlay in the kmz archive
                int mapLayerOrder = mapContent.layers().indexOf(layer);
                String href = "images/layers_" + mapLayerOrder + ".png";
                context.addKmzGroundOverlay(href, layer);
                return href;
            } else {
                // refer to a GetMap request
                return WMSRequests.getGetMapUrl(
                        mapContent.getRequest(),
                        layer,
                        0,
                        mapContent.getRenderingArea(),
                        new String[] {"format", "image/png", "transparent", "true"});
            }
        }

        /**
         * Determines whether to return a vector (KML) result of the data or to return an image
         * instead. If the kmscore is 100, then the output should always be vector. If the kmscore
         * is 0, it should always be raster. In between, the number of features is weighed against
         * the kmscore value. kmscore determines whether to return the features as vectors, or as
         * one raster image. It is the point, determined by the user, where X number of features is
         * "too many" and the result should be returned as an image instead.
         *
         * <p>kmscore is logarithmic. The higher the value, the more features it takes to make the
         * algorithm return an image. The lower the kmscore, the fewer features it takes to force an
         * image to be returned. (in use, the formula is exponential: as you increase the KMScore
         * value, the number of features required increases exponentially).
         *
         * @return true: use just kml vectors, false: use raster result
         */
        boolean useVectorOutput(KmlEncodingContext context) {
            // are we in download mode?
            String mode = context.getMode();
            if ("refresh".equalsIgnoreCase(mode)) {
                // calculate kmscore to determine if we should write as vectors
                // or pre-render
                int kmscore = context.getKmScore();

                if (kmscore == 100) {
                    return true; // vector KML
                }

                if (kmscore == 0) {
                    return false; // raster KMZ
                }

                // For numbers in between, determine exponentionally based on kmscore value:
                // 10^(kmscore/15)
                // This results in exponential growth.
                // The lowest bound is 1 feature and the highest bound is 3.98 million features
                // The most useful kmscore values are between 20 and 70 (21 and 46000 features
                // respectively)
                // A good default kmscore value is around 40 (464 features)
                double magic = Math.pow(10, kmscore / 15);

                int currentSize = context.getCurrentFeatureCollection().size();
                if (currentSize > magic) {
                    return false; // return raster
                } else {
                    return true; // return vector
                }
            } else {
                // download or superoverlay
                return true;
            }
        }
    }
}
