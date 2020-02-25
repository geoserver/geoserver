/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wcs11;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.ows11.CodeType;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geotools.xsd.EMFUtils;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

public class GetCoverageHandler extends RequestObjectHandler {

    static Logger LOGGER = Logging.getLogger("org.geoserver.monitor");

    public GetCoverageHandler(MonitorConfig config) {
        super("net.opengis.wcs11.GetCoverageType", config);
    }

    @Override
    public List<String> getLayers(Object request) {
        CodeType id = (CodeType) EMFUtils.get((EObject) request, "identifier");
        return id != null ? Arrays.asList(id.getValue()) : null;
    }

    @Override
    protected BoundingBox getBBox(Object request) {

        Object domainSubset = OwsUtils.get(request, "domainSubset");
        Object wcsBbox = OwsUtils.get(domainSubset, "boundingBox");

        @SuppressWarnings("unchecked")
        List<Double> upperCorner = (List<Double>) OwsUtils.get(wcsBbox, "upperCorner");
        @SuppressWarnings("unchecked")
        List<Double> lowerCorner = (List<Double>) OwsUtils.get(wcsBbox, "lowerCorner");

        CoordinateReferenceSystem crs = null;
        String crsName = (String) OwsUtils.get(wcsBbox, "crs");
        try {
            crs = CRS.decode(crsName);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, String.format("Could not decode CRS ID: %s", crsName), e);
            return null;
        }

        double minX = lowerCorner.get(0);
        double maxX = upperCorner.get(0);
        double minY = lowerCorner.get(1);
        double maxY = upperCorner.get(1);

        try {
            // Turn into a class that implements BoundingBox
            return new ReferencedEnvelope(minX, maxX, minY, maxY, crs)
                    .toBounds(monitorConfig.getBboxCrs());
        } catch (TransformException e) {
            LOGGER.log(Level.WARNING, "Could not transform bounding box to logging CRS", e);
            return null;
        }
    }
}
