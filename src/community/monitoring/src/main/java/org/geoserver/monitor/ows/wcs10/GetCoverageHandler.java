/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wcs10;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.xml.EMFUtils;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.Envelope;

public class GetCoverageHandler extends RequestObjectHandler {

    public GetCoverageHandler() {
        super("net.opengis.wcs10.GetCoverageType");
    }

    @Override
    public List<String> getLayers(Object request) {
        String source = (String)EMFUtils.get((EObject)request, "sourceCoverage");
        return source != null ? Arrays.asList(source) : null;
    }
    
    @Override
    protected BoundingBox getBBox(Object request) {
        
        Object domainSubset = OwsUtils.get(request, "domainSubset");
        Object spatialSubset = OwsUtils.get(domainSubset, "spatialSubset");
        
        
        if(spatialSubset==null) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
		List<Envelope> envelopes = (List<Envelope>) OwsUtils.get(spatialSubset, "envelope");
        
        // According to the WCS spec there should be exactly one
        Envelope env = envelopes.get(0);
        
        // Turn into a class that implements BoundingBox
        return new ReferencedEnvelope(env);
    }

}
