/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wcs10;

import java.util.Arrays;
import java.util.List;

import net.opengis.wcs10.GetCoverageType;
import net.opengis.wcs10.SpatialSubsetType;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.monitor.ows.RequestObjectHandler;
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
        GetCoverageType gcRequest = (GetCoverageType) request;
        
        // Domain subset may contain a spatial subset.
        SpatialSubsetType spatialSubset = gcRequest.getDomainSubset().getSpatialSubset();
        
        if(spatialSubset==null) {
            return null;
        }
        
        // If there is a spatial subset, it should contain exactly one OpenGIS Envelope.
        // This needs to be converted to a BoundingBox (implemented by ReferencedEnvelope)
        return new ReferencedEnvelope((Envelope) spatialSubset.getEnvelope().get(0));
    }

}
