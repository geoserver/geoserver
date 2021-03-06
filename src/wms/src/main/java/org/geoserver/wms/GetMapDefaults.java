/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.ServiceException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.projection.ProjectionException;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** Can be used to fill in defaults for incomplete GetMap requests */
public class GetMapDefaults {

    private static final Logger LOGGER = Logging.getLogger(GetMapDefaults.class);

    private int maxSide = DefaultWebMapService.MAX_SIDE;
    private int maxOpenLayersWidth = DefaultWebMapService.MAX_OL_WIDTH;
    private int minOpenlayersWidth = DefaultWebMapService.MIN_OL_WIDTH;
    private int minOpenLayersHeight = DefaultWebMapService.MIN_OL_HEIGHT;
    private int maxOpenLayersHeight = DefaultWebMapService.MAX_OL_HEIGHT;

    public GetMapRequest autoSetMissingProperties(GetMapRequest getMap) {
        // set the defaults
        if (getMap.getFormat() == null) {
            getMap.setFormat(DefaultWebMapService.FORMAT);
        }

        if ((getMap.getStyles() == null) || getMap.getStyles().isEmpty()) {
            // set styles to be the defaults for the specified layers
            // TODO: should this be part of core WMS logic? is so lets throw
            // this
            // into the GetMapKvpRequestReader
            if ((getMap.getLayers() != null) && (getMap.getLayers().size() > 0)) {
                ArrayList<Style> styles = new ArrayList<>(getMap.getLayers().size());

                for (int i = 0; i < getMap.getLayers().size(); i++) {
                    styles.add(getMap.getLayers().get(i).getDefaultStyle());
                }

                getMap.setStyles(styles);
            } else {
                getMap.setStyles(DefaultWebMapService.STYLES);
            }
        }

        // auto-magic missing info configuration
        autoSetBoundsAndSize(getMap);

        return getMap;
    }

    /**
     * This method tries to automatically determine SRS, bounding box and output size based on the
     * layers provided by the user and any other parameters.
     *
     * <p>If bounds are not specified by the user, they are automatically se to the union of the
     * bounds of all layers.
     *
     * <p>The size of the output image defaults to 512 pixels, the height is automatically
     * determined based on the width to height ratio of the requested layers. This is also true if
     * either height or width are specified by the user. If both height and width are specified by
     * the user, the automatically determined bounding box will be adjusted to fit inside these
     * bounds.
     *
     * <p>General idea 1) Figure out whether SRS has been specified, fall back to EPSG:4326 2)
     * Determine whether all requested layers use the same SRS, - if so, try to do bounding box
     * calculations in native coordinates 3) Aggregate the bounding boxes (in EPSG:4326 or native)
     * 4a) If bounding box has been specified, adjust height of image to match 4b) If bounding box
     * has not been specified, but height has, adjust bounding box
     */
    public void autoSetBoundsAndSize(GetMapRequest getMap) {
        // Get the layers
        List<MapLayerInfo> layers = getMap.getLayers();

        /** 1) Check what SRS has been requested */
        String reqSRS = getMap.getSRS();

        // if none, try to determine which SRS to use
        // and keep track of whether we can use native all the way
        boolean useNativeBounds = true;
        if (reqSRS == null) {
            reqSRS = guessCommonSRS(layers);
            forceSRS(getMap, reqSRS);
        }

        /** 2) Compare requested SRS */
        for (int i = 0; useNativeBounds && i < layers.size(); i++) {
            if (layers.get(i) != null) {
                String layerSRS = layers.get(i).getSRS();
                useNativeBounds =
                        reqSRS.equalsIgnoreCase(layerSRS)
                                && layers.get(i).getResource().getNativeBoundingBox() != null;
            } else {
                useNativeBounds = false;
            }
        }

        CoordinateReferenceSystem reqCRS;
        try {
            reqCRS = CRS.decode(reqSRS);
        } catch (Exception e) {
            throw new ServiceException(e);
        }

        // Ready to determine the bounds based on the layers, if not specified
        Envelope aggregateBbox = getMap.getBbox();
        boolean specifiedBbox = true;

        // If bbox is not specified by request
        if (aggregateBbox == null) {
            specifiedBbox = false;

            // Get the bounding box from the layers
            for (MapLayerInfo layerInfo : layers) {
                ReferencedEnvelope curbbox;
                try {
                    curbbox = layerInfo.getLatLongBoundingBox();
                    if (useNativeBounds) {
                        ReferencedEnvelope nativeBbox = layerInfo.getBoundingBox();
                        if (nativeBbox == null) {
                            try {
                                CoordinateReferenceSystem nativeCrs =
                                        layerInfo.getCoordinateReferenceSystem();
                                nativeBbox = curbbox.transform(nativeCrs, true);
                            } catch (Exception e) {
                                throw new ServiceException(
                                        "Best effort native bbox computation failed", e);
                            }
                        }
                        curbbox = nativeBbox;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (aggregateBbox != null) {
                    aggregateBbox.expandToInclude(curbbox);
                } else {
                    // defensive copy (otherwise it can cause a catalog referenced envelope to be
                    // modified)
                    aggregateBbox = new ReferencedEnvelope(curbbox);
                }
            }

            ReferencedEnvelope ref = null;
            // Reproject back to requested SRS if we have to
            if (!useNativeBounds && !reqSRS.equalsIgnoreCase(DefaultWebMapService.SRS)) {
                try {
                    ref = new ReferencedEnvelope(aggregateBbox, CRS.decode("EPSG:4326"));
                    aggregateBbox = ref.transform(reqCRS, true);
                } catch (ProjectionException pe) {
                    ref.expandBy(-1 * ref.getWidth() / 50, -1 * ref.getHeight() / 50);
                    try {
                        aggregateBbox = ref.transform(reqCRS, true);
                    } catch (Exception e) {
                        LOGGER.log(Level.FINE, "Failed to aggregate box", e);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Failed to aggregate box", e);
                }
            }
        }

        // Just in case
        if (aggregateBbox == null) {
            forceSRS(getMap, DefaultWebMapService.SRS);
            aggregateBbox = DefaultWebMapService.BBOX;
        }

        // Start the processing of adjust either the bounding box
        // or the pixel height / width

        double bbheight = aggregateBbox.getHeight();
        double bbwidth = aggregateBbox.getWidth();
        double bbratio = bbwidth / bbheight;

        double mheight = getMap.getHeight();
        double mwidth = getMap.getWidth();

        if (mheight <= 0.5 || mwidth <= 0.5 || !specifiedBbox) {
            if (mheight > 0.5 && mwidth > 0.5) {
                // Fully specified, need to adjust bbox
                double mratio = mwidth / mheight;
                // Adjust bounds to be less than ideal to meet spec
                if (bbratio > mratio) {
                    // Too wide, need to increase height of bb
                    double diff = ((bbwidth / mratio) - bbheight) / 2;
                    aggregateBbox.expandBy(0, diff);
                } else {
                    // Too tall, need to increase width of bb
                    double diff = ((bbheight * mratio) - bbwidth) / 2;
                    aggregateBbox.expandBy(diff, 0);
                }
                adjustBounds(reqSRS, aggregateBbox);
            } else if (mheight > 0.5) {
                mwidth = bbratio * mheight;
            } else {
                if (mwidth > 0.5) {
                    mheight = (mwidth / bbratio >= 1) ? mwidth / bbratio : 1;
                } else {
                    if (bbratio > 1) {
                        mwidth = maxSide;
                        mheight = (mwidth / bbratio >= 1) ? mwidth / bbratio : 1;
                    } else {
                        mheight = maxSide;
                        mwidth = (mheight * bbratio >= 1) ? mheight * bbratio : 1;
                    }

                    // OL specific adjustments
                    if ("application/openlayers".equalsIgnoreCase(getMap.getFormat())
                            || "openlayers".equalsIgnoreCase(getMap.getFormat())) {
                        if (mheight < minOpenLayersHeight) {
                            mheight = minOpenLayersHeight;
                        } else {
                            if (mheight > maxOpenLayersHeight) {
                                mheight = maxOpenLayersHeight;
                            }
                        }
                        if (mwidth < minOpenlayersWidth) {
                            mwidth = minOpenlayersWidth;
                        } else if (mwidth > maxOpenLayersWidth) {
                            mwidth = maxOpenLayersWidth;
                        }
                    }
                }
            }

            // Actually set the bounding box and size of image
            getMap.setBbox(aggregateBbox);
            getMap.setWidth((int) mwidth);
            getMap.setHeight((int) mheight);
        }
    }

    private static String guessCommonSRS(List<MapLayerInfo> layers) {
        String SRS = null;
        for (MapLayerInfo layer : layers) {
            String layerSRS = layer.getSRS();
            if (SRS == null) {
                SRS = layerSRS.toUpperCase();
            } else if (!SRS.equals(layerSRS)) {
                // layers with mixed native SRS, let's just use the default
                return DefaultWebMapService.SRS;
            }
        }
        if (SRS == null) {
            return DefaultWebMapService.SRS;
        }
        return SRS;
    }

    private static void forceSRS(GetMapRequest getMap, String srs) {
        getMap.setSRS(srs);

        try {
            getMap.setCrs(CRS.decode(srs));
        } catch (FactoryException e) {
            LOGGER.log(Level.WARNING, "", e);
        }
    }

    /**
     * This adjusts the bounds by zooming out 2%, but also ensuring that the maximum bounds do not
     * exceed the world bounding box
     *
     * <p>This only applies if the SRS is EPSG:4326 or EPSG:900913
     *
     * @param reqSRS the SRS
     * @param bbox the current bounding box
     * @return the adjusted bounding box
     */
    private Envelope adjustBounds(String reqSRS, Envelope bbox) {
        bbox.expandBy(bbox.getWidth() / 100, bbox.getHeight() / 100);
        if (reqSRS.equalsIgnoreCase("EPSG:4326")) {
            Envelope maxEnv = new Envelope(-180.0, -90.0, 180.0, 90.0);
            return bbox.intersection(maxEnv);
        } else if (reqSRS.equalsIgnoreCase("EPSG:900913")) {
            Envelope maxEnv = new Envelope(-20037508.33, -20037508.33, 20037508.33, 20037508.33);
            return bbox.intersection(maxEnv);
        }
        return bbox;
    }

    public int getMaxOpenLayersWidth() {
        return maxOpenLayersWidth;
    }

    public void setMaxOpenLayersWidth(int maxOpenLayersWidth) {
        this.maxOpenLayersWidth = maxOpenLayersWidth;
    }

    public int getMinOpenlayersWidth() {
        return minOpenlayersWidth;
    }

    public void setMinOpenlayersWidth(int minOpenlayersWidth) {
        this.minOpenlayersWidth = minOpenlayersWidth;
    }

    public int getMaxSide() {
        return maxSide;
    }

    public void setMaxSide(int maxSide) {
        this.maxSide = maxSide;
    }

    public int getMaxOpenLayersHeight() {
        return maxOpenLayersHeight;
    }

    public void setMaxOpenLayersHeight(int maxOpenLayersHeight) {
        this.maxOpenLayersHeight = maxOpenLayersHeight;
    }
}
