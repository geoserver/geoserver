/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import static org.geoserver.wms.map.StyleQueryUtil.buildTransform;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import org.geoserver.wms.WMSMapContent;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.util.ScreenMap;
import org.geotools.geometry.jts.Decimator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.operation.transform.ConcatenatedTransform;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.renderer.lite.RendererUtilities;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

/** Support class that can simplify geometries and help querying them with the right resolution */
class MapMLSimplifier {

    /**
     * The tolerance in pixels, two vertices at less than this distance will be considered the same. It's smaller
     * compared to the server side renderer, the client side seems to render thinner lines.
     */
    public static final double PIXEL_TOLERANCE = 0.1;

    private final double querySimplificationDistance;
    private final double simplificationDistance;
    private final ScreenMap screenMap;
    private final ReferencedEnvelope renderingArea;

    public MapMLSimplifier(WMSMapContent mapContent, CoordinateReferenceSystem sourceCrs) throws FactoryException {
        Rectangle paintArea = new Rectangle(mapContent.getMapWidth(), mapContent.getMapHeight());
        this.renderingArea = mapContent.getRenderingArea();
        AffineTransform worldToScreen = RendererUtilities.worldToScreenTransform(renderingArea, paintArea);

        CoordinateReferenceSystem mapCrs = renderingArea.getCoordinateReferenceSystem();
        MathTransform sourceToTargetCrs = buildTransform(sourceCrs, mapCrs);
        MathTransform targetToScreen = ProjectiveTransform.create(worldToScreen);
        MathTransform sourceToScreen = ConcatenatedTransform.create(sourceToTargetCrs, targetToScreen);

        double[] spans_sourceCRS;
        double[] spans_targetCRS;
        try {
            spans_sourceCRS =
                    Decimator.computeGeneralizationDistances(sourceToScreen.inverse(), paintArea, PIXEL_TOLERANCE);

            spans_targetCRS =
                    Decimator.computeGeneralizationDistances(targetToScreen.inverse(), paintArea, PIXEL_TOLERANCE);

        } catch (TransformException e) {
            throw new RuntimeException(e);
        }

        // use min so generalize "less" (if pixel is different size in X and Y)
        querySimplificationDistance = Math.min(spans_sourceCRS[0], spans_sourceCRS[1]);

        // use min so generalize "less" (if pixel is different size in X and Y)
        simplificationDistance = Math.min(spans_targetCRS[0], spans_targetCRS[1]);

        this.screenMap = new ScreenMap(0, 0, paintArea.width, paintArea.height);
        screenMap.setSpans(spans_sourceCRS[0], spans_sourceCRS[1]);
        screenMap.setTransform(sourceToScreen);
    }

    public double getQuerySimplificationDistance() {
        return querySimplificationDistance;
    }

    public Geometry simplify(Geometry geom) throws FactoryException, TransformException {
        if (geom == null) return null;
        Geometry result = geom;

        // TODO: projection handling TBD
        //        if (this.projectionHandler != null) {
        //            result = projectionHandler.preProcess(geom);
        //            if (result == null) return null;
        //        }

        // if line or polygon, see if it's smaller than a pixel that's already busy
        int dimension = result.getDimension();
        if (dimension > 0) {
            Envelope env = result.getEnvelopeInternal();
            if (screenMap.canSimplify(env)) {
                if (screenMap.checkAndSet(env)) {
                    return null;
                } else {
                    result = screenMap.getSimplifiedShape(
                            env.getMinX(),
                            env.getMinY(),
                            env.getMaxX(),
                            env.getMaxY(),
                            result.getFactory(),
                            result.getClass());
                }
            } else if (dimension == 2) {
                result = TopologyPreservingSimplifier.simplify(geom, this.simplificationDistance);
            } else if (dimension == 1) {
                result = DouglasPeuckerSimplifier.simplify(geom, this.simplificationDistance);
            }
        }

        return result;
    }
}
