/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wcs10;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.geotools.xsd.EMFUtils;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.TransformException;

public class GetCoverageHandler extends RequestObjectHandler {

    static Logger LOGGER = Logging.getLogger("org.geoserver.monitor");

    public GetCoverageHandler(MonitorConfig config) {
        super("net.opengis.wcs10.GetCoverageType", config);
    }

    @Override
    public List<String> getLayers(Object request) {
        String source = (String) EMFUtils.get((EObject) request, "sourceCoverage");
        return source != null ? Arrays.asList(source) : null;
    }

    @Override
    protected BoundingBox getBBox(Object request) {

        Object domainSubset = OwsUtils.get(request, "domainSubset");
        Object spatialSubset = OwsUtils.get(domainSubset, "spatialSubset");

        if (spatialSubset == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        List<Envelope> envelopes = (List<Envelope>) OwsUtils.get(spatialSubset, "envelope");

        // According to the WCS spec there should be exactly one
        Envelope env = envelopes.get(0);

        BoundingBox result = null;
        // Turn into a class that implements BoundingBox
        try {
            result = new ReferencedEnvelope(env).toBounds(monitorConfig.getBboxCrs());
        } catch (TransformException e) {
            LOGGER.log(Level.WARNING, "Could not transform bounding box to logging CRS", e);
            return null;
        }
        return result;
    }
}
