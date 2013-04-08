/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.sequence;

import java.util.List;

import org.geoserver.kml.KMLUtils;
import org.geoserver.kml.decorator.KmlDecoratorFactory.KmlDecorator;
import org.geoserver.kml.decorator.KmlEncodingContext;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSRequests;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.GroundOverlay;
import de.micromata.opengis.kml.v_2_2_0.Icon;
import de.micromata.opengis.kml.v_2_2_0.LatLonBox;
import de.micromata.opengis.kml.v_2_2_0.ViewRefreshMode;

/**
 * Creates a sequence of folders mapping the layers in the map content
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class FolderSequenceFactory implements SequenceFactory<Feature> {

    private KmlEncodingContext context;

    private List<KmlDecorator> decorators;

    public FolderSequenceFactory(KmlEncodingContext context) {
        this.context = context;
        this.decorators = context.getDecoratorsForClass(Folder.class);
    }

    @Override
    public Sequence<Feature> newSequence() {
        return new FolderGenerator();
    }

    public class FolderGenerator implements Sequence<Feature> {
        int i = 0;

        int size;

        public FolderGenerator() {
            this.size = context.getMapContent().layers().size();
        }

        @Override
        public Feature next() {
            while (i < size) {
                List<Layer> layers = context.getMapContent().layers();
                Layer layer = layers.get(i++);
                context.setCurrentLayer(layer);

                if (layer instanceof FeatureLayer) {
                    try {
                        WMSMapContent mapContent = context.getMapContent();
                        SimpleFeatureCollection fc = KMLUtils.loadFeatureCollection(
                                (SimpleFeatureSource) layer.getFeatureSource(), layer, mapContent,
                                context.getWms(), mapContent.getScaleDenominator());
                        context.setCurrentFeatureCollection(fc);
                    } catch (Exception e) {
                        if (e instanceof ServiceException) {
                            throw (ServiceException) e;
                        } else {
                            throw new ServiceException(
                                    "Failed to load vector data during KML generation", e);
                        }
                    }
                }

                // setup the folder and let it be decorated
                Folder folder = new Folder();
                folder.setName(layer.getTitle());
                for (KmlDecorator decorator : decorators) {
                    folder = (Folder) decorator.decorate(folder, context);
                    if (folder == null) {
                        continue;
                    }
                }

                if (layer instanceof FeatureLayer) {
                    if (useVectorOutput(context.getCurrentFeatureCollection())) {
                        List<Feature> features = new SequenceList<Feature>(
                                new FeatureSequenceFactory(context, (FeatureLayer) layer));
                        addFeatures(folder, features);
                    } else {
                        addGroundOverlay(folder, layer);
                    }
                } else {
                    addGroundOverlay(folder, layer);
                }

                return folder;
            }
            return null;
        }

        /**
         * Adds features to the folder own list
         * @param folder
         * @param features
         */
        void addFeatures(Folder folder, List<Feature> features) {
            List<Feature> originalFeatures = folder.getFeature();
            if (originalFeatures == null || originalFeatures.size() == 0) {
                folder.setFeature(features);
            } else {
                // in this case, compose the already existing features with the
                // dynamically
                // generated ones
                folder.setFeature(new CompositeList<Feature>(originalFeatures, features));
            }
        }

        private void addGroundOverlay(Folder folder, Layer layer) {
            int mapLayerOrder = context.getMapContent().layers().indexOf(layer);

            GroundOverlay go = folder.createAndAddGroundOverlay();
            go.setName(layer.getTitle());
            go.setDrawOrder(mapLayerOrder);
            Icon icon = go.createAndSetIcon();
            icon.setHref(getGroundOverlayHRef(layer));
            icon.setViewRefreshMode(ViewRefreshMode.NEVER);
            icon.setViewBoundScale(0.75);

            ReferencedEnvelope box = new ReferencedEnvelope(context.getMapContent()
                    .getRenderingArea());
            boolean reprojectBBox = (box.getCoordinateReferenceSystem() != null)
                    && !CRS.equalsIgnoreMetadata(box.getCoordinateReferenceSystem(),
                            DefaultGeographicCRS.WGS84);
            if (reprojectBBox) {
                try {
                    box = box.transform(DefaultGeographicCRS.WGS84, true);
                } catch (Exception e) {
                    throw new ServiceException("Could not transform bbox to WGS84", e,
                            "ReprojectionError", "");
                }
            }

            LatLonBox gobox = go.createAndSetLatLonBox();
            gobox.setEast(box.getMinX());
            gobox.setWest(box.getMaxX());
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
                return WMSRequests.getGetMapUrl(mapContent.getRequest(), layer, 0,
                        mapContent.getRenderingArea(), new String[] { "format", "image/png",
                                "transparent", "true" });
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
         * kmscore is logarithmic. The higher the value, the more features it takes to make the
         * algorithm return an image. The lower the kmscore, the fewer features it takes to force an
         * image to be returned. (in use, the formula is exponential: as you increase the KMScore
         * value, the number of features required increases exponentially).
         * 
         * @param kmscore the score, between 0 and 100, use to determine what output to use
         * @param numFeatures how many features are being rendered
         * @return true: use just kml vectors, false: use raster result
         */
        boolean useVectorOutput(SimpleFeatureCollection fc) {
            // calculate kmscore to determine if we shoud write as vectors
            // or pre-render
            int kmscore = context.getWms().getKmScore();
            Object kmScoreObj = context.getRequest().getFormatOptions().get("kmscore");
            if (kmScoreObj != null) {
                kmscore = (Integer) kmScoreObj;
            }

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

            if (fc.size() > magic) {
                return false; // return raster
            } else {
                return true; // return vector
            }
        }

    }

}
