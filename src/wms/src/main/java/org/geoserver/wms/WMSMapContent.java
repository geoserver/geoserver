/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.platform.ServiceException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StreamingRenderer;

/**
 * Extends DefaultMapContext to provide the whole set of request parameters a WMS GetMap request can
 * have.
 *
 * <p>In particular, adds holding for the following parameter values:
 *
 * <ul>
 *   <li>WIDTH
 *   <li>HEIGHT
 *   <li>BGCOLOR
 *   <li>TRANSPARENT
 * </ul>
 *
 * @author Gabriel Roldan
 * @author Simone Giannecchini - GeoSolutions SAS
 * @version $Id$
 */
public class WMSMapContent extends MapContent {
    /** requested map image width in output units (pixels) */
    private int mapWidth;

    /** requested map image height in output units (pixels) */
    private int mapHeight;

    /** Requested BGCOLOR, defaults to white according to WMS spec */
    private Color bgColor = Color.white;

    /** true if background transparency is requested */
    private boolean transparent;

    /** suggested output tile size */
    private int tileSize = -1;

    /** map rotation in degrees */
    private double angle;

    private List<GetMapCallback> callbacks;

    private Map<String, Object> metadata = new HashMap<>();

    public int getTileSize() {
        return tileSize;
    }

    public void setTileSize(int tileSize) {
        this.tileSize = tileSize;
    }

    /**
     * the rendering buffer used to avoid issues with tiled rendering and big strokes that may cross
     * tile boundaries
     */
    private int buffer;

    /** The {@link IndexColorModel} the user required for the resulting map */
    private IndexColorModel icm;

    private GetMapRequest request; // hold onto it so we can grab info from it

    // (request URL etc...)

    public WMSMapContent() {
        super();
    }

    public WMSMapContent(GetMapRequest req) {
        super();
        request = req;
    }

    public WMSMapContent(WMSMapContent other, boolean copyLayers) {
        this.mapWidth = other.mapWidth;
        this.mapHeight = other.mapHeight;
        this.bgColor = other.bgColor;
        this.transparent = other.transparent;
        this.tileSize = other.tileSize;
        this.angle = other.angle;
        this.callbacks = new ArrayList<>(other.callbacks);
        this.buffer = other.buffer;
        this.icm = other.icm;
        this.request = other.request;
        if (copyLayers) {
            this.layers().addAll(other.layers());
        }
        this.getViewport().setBounds(other.getViewport().getBounds());
    }

    public Color getBgColor() {
        return this.bgColor;
    }

    public void setBgColor(Color bgColor) {
        this.bgColor = bgColor;
    }

    public int getMapHeight() {
        return this.mapHeight;
    }

    public void setMapHeight(int mapHeight) {
        this.mapHeight = mapHeight;
    }

    public int getMapWidth() {
        return this.mapWidth;
    }

    public void setMapWidth(int mapWidth) {
        this.mapWidth = mapWidth;
    }

    public boolean isTransparent() {
        return this.transparent;
    }

    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }

    public GetMapRequest getRequest() {
        return request;
    }

    public void setRequest(GetMapRequest request) {
        this.request = request;
    }

    public int getBuffer() {
        return buffer;
    }

    public void setBuffer(int buffer) {
        this.buffer = buffer;
    }

    public IndexColorModel getPalette() {
        return icm;
    }

    public void setPalette(IndexColorModel paletteInverter) {
        this.icm = paletteInverter;
    }

    /** The clockwise rotation angle of the map, in degrees */
    public double getAngle() {
        return angle;
    }

    public void setAngle(double rotation) {
        this.angle = rotation;
    }

    @Override
    public boolean addLayer(Layer layer) {
        layer = fireLayerCallbacks(layer);
        if (layer != null) {
            return super.addLayer(layer);
        } else {
            return false;
        }
    }

    private Layer fireLayerCallbacks(Layer layer) {
        // if no callbacks, return the layer as is
        if (callbacks == null) {
            return layer;
        }

        // process through the callbacks
        for (GetMapCallback callback : callbacks) {
            layer = callback.beforeLayer(this, layer);
            if (layer == null) {
                return null;
            }
        }

        return layer;
    }

    @Override
    public int addLayers(Collection<? extends Layer> layers) {
        List<Layer> filtered = new ArrayList<Layer>(layers.size());
        for (Layer layer : layers) {
            layer = fireLayerCallbacks(layer);
            if (layer != null) {
                filtered.add(layer);
            }
        }

        if (filtered.size() > 0) {
            return super.addLayers(filtered);
        } else {
            return 0;
        }
    }

    /**
     * Returns the transformation going from the map area space to the screen space taking into
     * account map rotation
     */
    public AffineTransform getRenderingTransform() {
        Rectangle paintArea = new Rectangle(0, 0, getMapWidth(), getMapHeight());
        ReferencedEnvelope dataArea = getViewport().getBounds();
        AffineTransform tx;
        if (getAngle() != 0.0) {
            tx = new AffineTransform();
            tx.translate(paintArea.width / 2, paintArea.height / 2);
            tx.rotate(Math.toRadians(getAngle()));
            tx.translate(-paintArea.width / 2, -paintArea.height / 2);
            tx.concatenate(RendererUtilities.worldToScreenTransform(dataArea, paintArea));
        } else {
            tx = RendererUtilities.worldToScreenTransform(dataArea, paintArea);
        }
        return tx;
    }

    /**
     * Returns the actual area that should be drawn taking into account the map rotation account map
     * rotation
     */
    public ReferencedEnvelope getRenderingArea() {
        ReferencedEnvelope dataArea = getViewport().getBounds();
        if (getAngle() == 0) return dataArea;

        AffineTransform tx = new AffineTransform();
        double offsetX = dataArea.getMinX() + dataArea.getWidth() / 2;
        double offsetY = dataArea.getMinY() + dataArea.getHeight() / 2;
        tx.translate(offsetX, offsetY);
        tx.rotate(Math.toRadians(getAngle()));
        tx.translate(-offsetX, -offsetY);
        Rectangle2D dataAreaShape =
                new Rectangle2D.Double(
                        dataArea.getMinX(),
                        dataArea.getMinY(),
                        dataArea.getWidth(),
                        dataArea.getHeight());
        Rectangle2D transformedBounds = tx.createTransformedShape(dataAreaShape).getBounds2D();
        return new ReferencedEnvelope(transformedBounds, dataArea.getCoordinateReferenceSystem());
    }

    /**
     * Get the contact information associated with this context, returns an empty string if
     * contactInformation has not been set.
     *
     * @return the ContactInformation or an empty string if not present
     */
    public String getContactInformation() {
        String contact = (String) getUserData().get("contact");
        return contact == null ? "" : contact;
    }

    /**
     * Set contact information associated with this class.
     *
     * @param contactInformation the ContactInformation.
     */
    public void setContactInformation(final String contactInformation) {
        getUserData().put("contact", contactInformation);
    }

    /**
     * Get an array of keywords associated with this context, returns an empty array if no keywords
     * have been set. The array returned is a copy, changes to the returned array won't influence
     * the MapContextState
     *
     * @return array of keywords
     */
    public String[] getKeywords() {
        Object obj = getUserData().get("keywords");
        if (obj == null) {
            return new String[0];
        } else if (obj instanceof String) {
            String keywords = (String) obj;
            return keywords.split(",");
        } else if (obj instanceof String[]) {
            String keywords[] = (String[]) obj;
            String[] copy = new String[keywords.length];
            System.arraycopy(keywords, 0, copy, 0, keywords.length);
            return copy;
        } else if (obj instanceof Collection) {
            Collection<String> keywords = (Collection) obj;
            return keywords.toArray(new String[keywords.size()]);
        } else {
            return new String[0];
        }
    }

    /**
     * Set an array of keywords to associate with this context.
     *
     * @param keywords the Keywords.
     */
    public void setKeywords(final String[] keywords) {
        getUserData().put("keywords", keywords);
    }

    /**
     * Get the abstract which describes this interface, returns an empty string if this has not been
     * set yet.
     *
     * @return The Abstract or an empty string if not present
     */
    public String getAbstract() {
        String description = (String) getUserData().get("abstract");
        return description == null ? "" : description;
    }

    /**
     * Set an abstract which describes this context.
     *
     * @param contextAbstract the Abstract.
     */
    public void setAbstract(final String contextAbstract) {
        getUserData().put("abstract", contextAbstract);
    }

    public void setGetMapCallbacks(final List<GetMapCallback> callbacks) {
        this.callbacks = callbacks;
    }

    public double getScaleDenominator() {
        return getScaleDenominator(false);
    }

    public double getScaleDenominator(boolean considerDPI) {
        java.util.Map hints = new HashMap();
        if (considerDPI) {
            // compute the DPI
            if (request.getFormatOptions().get("dpi") != null) {
                hints.put(StreamingRenderer.DPI_KEY, (request.getFormatOptions().get("dpi")));
            }
        }
        if (request.getScaleMethod() == ScaleComputationMethod.Accurate) {
            if (request.getAngle() != 0) {
                throw new ServiceException(
                        "Accurate scale computation is not supported when using the angle parameter. "
                                + "This functionality could be added, please provide a pull request for it ;-)");
            }
            try {
                return RendererUtilities.calculateScale(
                        getViewport().getBounds(), getMapWidth(), getMapHeight(), hints);
            } catch (Exception e) {
                throw new ServiceException("Failed to compute accurate scale denominator", e);
            }
        } else {
            AffineTransform at = getRenderingTransform();
            if (Math.abs(XAffineTransform.getRotation(at)) != 0.0) {
                return RendererUtilities.calculateOGCScaleAffine(
                        getCoordinateReferenceSystem(), at, hints);
            } else {
                return RendererUtilities.calculateOGCScale(
                        getViewport().getBounds(), getMapWidth(), hints);
            }
        }
    }

    /** Computes the StreamingRenderer scale computation method hint based on the current request */
    public String getRendererScaleMethod() {
        if (request.getScaleMethod() == ScaleComputationMethod.Accurate) {
            return StreamingRenderer.SCALE_ACCURATE;
        } else {
            return StreamingRenderer.SCALE_OGC;
        }
    }

    /**
     * Generic map attached to the map content, can be used to persist information around the life
     * cycle when the {@link WebMap} is not appropriate, or to persist state across the various
     * response callbacks
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public void dispose() {
        this.request = null;
        this.callbacks = null;
        this.metadata = null;
        super.dispose();
    }
}
