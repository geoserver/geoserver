/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapCallbackAdapter;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.api.style.Fill;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyleFactory;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.crs.ProjectionHandlerFinder;
import org.geotools.styling.SLD;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;

/**
 * Paints the area outside the valid area of the map projection on its own, so that {@code void-color} and
 * {@code void-transparent} can differ from {@code bgcolor} and {@code transparent} (OGC API - Maps,
 * {@code /req/background/void-color-definition} and {@code /req/background/void-transparent-definition}).
 *
 * <p>The paint travels from {@link MapsService} as a format option holding a {@link Color}, one key for each side of
 * the valid area boundary. At most one of the two is ever set: the map background is the one the renderer paints, and
 * the other side becomes a polygon layer over it. The layer goes under the data when it paints the valid area, and over
 * it otherwise, the renderer drawing no data outside the valid area anyway.
 */
@Component
public class VoidBackgroundCallback extends GetMapCallbackAdapter {

    private static final Logger LOGGER = Logging.getLogger(VoidBackgroundCallback.class);

    /** Format option asking for the area outside the valid area to be filled with the {@link Color} it holds. */
    static final String VOID_COLOR = "ogcapi-maps-void-color";

    /** Format option asking for the area inside the valid area to be filled with the {@link Color} it holds. */
    static final String BACKGROUND_COLOR = "ogcapi-maps-background-color";

    /** Points added along the valid area boundary before it is transformed into the map CRS. */
    private static final int BOUNDARY_POINTS = 720;

    /** Shrink applied before the transform, in degrees, so that the boundary stays inside the projection domain. */
    private static final double SHRINK = 1e-6;

    private static final Envelope WORLD = new Envelope(-180, 180, -90, 90);

    private static final StyleFactory STYLES = CommonFactoryFinder.getStyleFactory();

    private static final FilterFactory FILTERS = CommonFactoryFinder.getFilterFactory();

    private final WMS wms;

    /** Builds the callback with the WMS facade holding the advanced projection handling setting. */
    public VoidBackgroundCallback(WMS wms) {
        this.wms = wms;
    }

    @Override
    public WMSMapContent beforeRender(WMSMapContent content) {
        // a WMS client can spell the format option, it cannot fake an OGC API request
        if (APIRequestInfo.get() == null) return content;
        Color inside = (Color) content.getRequest().getFormatOptions().get(BACKGROUND_COLOR);
        Color outside = (Color) content.getRequest().getFormatOptions().get(VOID_COLOR);
        if (inside == null && outside == null) return content;
        // without advanced projection handling the renderer draws data outside the valid area too, and a mask
        // painted over it would hide map content
        if (!wms.isAdvancedProjectionHandlingEnabled()) return content;

        ReferencedEnvelope area = content.getRenderingArea();
        try {
            Geometry validArea = validArea(area);
            if (validArea == null) return content;
            Geometry projected = project(validArea, area);
            if (projected == null) return content;
            CoordinateReferenceSystem crs = area.getCoordinateReferenceSystem();
            if (inside != null) {
                content.layers().add(0, maskLayer(projected, crs, inside));
            } else {
                content.addLayer(maskLayer(JTS.toGeometry((Envelope) area).difference(projected), crs, outside));
            }
        } catch (FactoryException e) {
            throw new ServiceException("Failed to compute the valid area of " + area.getCoordinateReferenceSystem(), e);
        }
        return content;
    }

    /**
     * The valid area of the projection used to draw the map, in WGS84, or {@code null} when the projection covers the
     * whole world.
     */
    private static Geometry validArea(ReferencedEnvelope renderingArea) throws FactoryException {
        ProjectionHandler handler =
                ProjectionHandlerFinder.getHandler(renderingArea, DefaultGeographicCRS.WGS84, false);
        if (handler == null) return null;
        Geometry area = handler.getValidArea();
        if (area != null) return area;
        ReferencedEnvelope bounds = handler.getValidAreaBounds();
        // some handlers state a longitude range wider than the world to mean "any", which cannot be projected
        return bounds == null ? null : JTS.toGeometry(bounds.intersection(WORLD));
    }

    /**
     * The valid area in the CRS of the map, or {@code null} when it cannot be drawn on this map. Some projections
     * stretch their own limits so far that the transform refuses the points there, and the map then keeps its
     * background over the void.
     */
    private static Geometry project(Geometry validArea, ReferencedEnvelope renderingArea) throws FactoryException {
        Envelope bounds = validArea.getEnvelopeInternal();
        double step = Math.max(bounds.getWidth(), bounds.getHeight()) / BOUNDARY_POINTS;
        // the boundary is a curve in the map CRS, so it gets points added here: the renderer only densifies the
        // geometries it reprojects itself, and the mask is handed over already projected
        Geometry densified = Densifier.densify(validArea.buffer(-SHRINK), step);
        MathTransform toMap =
                CRS.findMathTransform(DefaultGeographicCRS.WGS84, renderingArea.getCoordinateReferenceSystem(), true);
        try {
            return JTS.transform(densified, toMap);
        } catch (TransformException e) {
            LOGGER.log(Level.FINE, e, () -> "Cannot project the valid area onto the map, leaving the void unpainted");
            return null;
        }
    }

    /** A single feature layer filling the given area with the given colour, with no outline. */
    private static Layer maskLayer(Geometry mask, CoordinateReferenceSystem crs, Color color) {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("mask");
        builder.setCRS(crs);
        builder.add("the_geom", Geometry.class);
        SimpleFeatureType type = builder.buildFeatureType();
        SimpleFeature feature = SimpleFeatureBuilder.build(type, new Object[] {mask}, "mask.1");
        return new FeatureLayer(DataUtilities.source(DataUtilities.collection(feature)), fillStyle(color));
    }

    /** An opaque polygon fill in the given colour, with no outline. */
    private static Style fillStyle(Color color) {
        Fill fill = STYLES.createFill(FILTERS.literal(color));
        return SLD.wrapSymbolizers(STYLES.createPolygonSymbolizer(null, fill, null));
    }
}
