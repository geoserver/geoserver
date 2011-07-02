/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Collection;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.palette.InverseColorMapOp;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.RendererUtilities;

/**
 * Extends DefaultMapContext to provide the whole set of request parameters a WMS GetMap request can
 * have.
 * 
 * <p>
 * In particular, adds holding for the following parameter values:
 * 
 * <ul>
 * <li>WIDTH</li>
 * <li>HEIGHT</li>
 * <li>BGCOLOR</li>
 * <li>TRANSPARENT</li>
 * </ul>
 * </p>
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

    /**
     * The {@link InverseColorMapOp} that actually does the color inversion.
     */
    private InverseColorMapOp paletteInverter;

    private GetMapRequest request; // hold onto it so we can grab info from it

    // (request URL etc...)

    public WMSMapContent() {
        super();
    }

    public WMSMapContent(GetMapRequest req) {
        super();
        request = req;
    }


    public WMSMapContent(Layer[] layers) {
        super();
        for (Layer layer : layers) {
            addLayer(layer);
        }
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

    public InverseColorMapOp getPaletteInverter() {
        return paletteInverter;
    }

    public void setPaletteInverter(InverseColorMapOp paletteInverter) {
        this.paletteInverter = paletteInverter;
    }

    /**
     * The clockwise rotation angle of the map, in degrees
     * 
     * @return
     */
    public double getAngle() {
        return angle;
    }

    public void setAngle(double rotation) {
        this.angle = rotation;
    }

    /**
     * Returns the transformation going from the map area space to the screen space taking into
     * account map rotation
     * 
     * @return
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
     * 
     * @return
     */
    public ReferencedEnvelope getRenderingArea() {
        ReferencedEnvelope dataArea = getViewport().getBounds(); 
        if (getAngle() == 0)
            return dataArea;

        AffineTransform tx = new AffineTransform();
        double offsetX = dataArea.getMinX() + dataArea.getWidth() / 2;
        double offsetY = dataArea.getMinY() + dataArea.getHeight() / 2;
        tx.translate(offsetX, offsetY);
        tx.rotate(Math.toRadians(getAngle()));
        tx.translate(-offsetX, -offsetY);
        Rectangle2D dataAreaShape = new Rectangle2D.Double(dataArea.getMinX(), dataArea.getMinY(),
                dataArea.getWidth(), dataArea.getHeight());
        Rectangle2D transformedBounds = tx.createTransformedShape(dataAreaShape).getBounds2D();
        return new ReferencedEnvelope(transformedBounds, dataArea.getCoordinateReferenceSystem());
    }
    
    /**
     * Get the contact information associated with this context, returns an empty string if
     * contactInformation has not been set.
     * 
     * @return the ContactInformation or an empty string if not present
     */
    public String getContactInformation(){
        String contact =  (String) getUserData().get("contact");
        return contact == null ? "" : contact;
    }

    /**
     * Set contact information associated with this class.
     * 
     * @param contactInformation
     *            the ContactInformation.
     */
    public void setContactInformation(final String contactInformation){
        getUserData().put("contact", contactInformation);
    }

    
    /**
     * Get an array of keywords associated with this context, returns an empty array if no keywords
     * have been set. The array returned is a copy, changes to the returned array won't influence
     * the MapContextState
     * 
     * @return array of keywords
     */
    public String[] getKeywords(){
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
     * @param keywords
     *            the Keywords.
     */
    public void setKeywords(final String[] keywords){
        getUserData().put("keywords", keywords);
    }
    
    /**
     * Get the abstract which describes this interface, returns an empty string if this has not been
     * set yet.
     * 
     * @return The Abstract or an empty string if not present
     */
    public String getAbstract(){
        String description = (String) getUserData().get("abstract");
        return description == null ? "" : description;
    }

    /**
     * Set an abstract which describes this context.
     * 
     * @param conAbstract
     *            the Abstract.
     */
    public void setAbstract(final String contextAbstract){
        getUserData().put("abstract", contextAbstract);
    }

}
