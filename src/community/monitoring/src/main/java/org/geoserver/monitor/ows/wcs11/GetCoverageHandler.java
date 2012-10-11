/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wcs11;

import java.util.Arrays;
import java.util.List;

import net.opengis.ows11.CodeType;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.xml.EMFUtils;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GetCoverageHandler extends RequestObjectHandler {

    public GetCoverageHandler(CoordinateReferenceSystem logCrs) {
        super("net.opengis.wcs11.GetCoverageType", logCrs);
    }
    
    @Override
    public List<String> getLayers(Object request) {
        CodeType id = (CodeType)EMFUtils.get((EObject)request, "identifier");
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
        	try {
				crs= CRS.decode((String) OwsUtils.get(wcsBbox, "crs"));
			} catch (Exception e) {
				// TODO: Log this or something.
			}
        

        double minX =  lowerCorner.get(0);
        double maxX =  upperCorner.get(0);
        double minY =  lowerCorner.get(1);
        double maxY =  upperCorner.get(1);
        	
        // Turn into a class that implements BoundingBox
        return new ReferencedEnvelope(minX, maxX, minY, maxY, crs);
        
    }

}
