/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.crs.ProjectionHandlerFinder;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * Extends ProjectionHandler in order to allow transforming a bbox into a target CRS when generating
 * the Capabilities, therefore dealing with potential transformation exceptions due to domains out
 * of validity.
 */
class CapabilitiesTransformerProjectionHandler extends ProjectionHandler {

    public CapabilitiesTransformerProjectionHandler(ProjectionHandler handler)
            throws FactoryException {
        super(handler.getSourceCRS(), handler.getValidAreaBounds(), handler.getRenderingEnvelope());
    }

    @Override
    protected ReferencedEnvelope transformEnvelope(
            ReferencedEnvelope envelope, CoordinateReferenceSystem targetCRS)
            throws TransformException, FactoryException {
        return super.transformEnvelope(envelope, targetCRS);
    }

    /**
     * Create a CapabilitiesTransformerProjectionHandler for transformations from sourceCrs to
     * targetCrs.
     *
     * @param sourceCrs the source CoordinateReferenceSystem
     * @param targetCrs the target CoordinateReferenceSystem
     * @return a proper ProjectionHandler or null if unable to get one.
     */
    public static CapabilitiesTransformerProjectionHandler create(
            CoordinateReferenceSystem sourceCrs, CoordinateReferenceSystem targetCrs)
            throws MismatchedDimensionException, FactoryException {
        ProjectionHandler handler =
                ProjectionHandlerFinder.getHandler(
                        new ReferencedEnvelope(targetCrs), sourceCrs, false);
        if (handler != null) {
            return new CapabilitiesTransformerProjectionHandler(handler);
        }
        return null;
    }
}
