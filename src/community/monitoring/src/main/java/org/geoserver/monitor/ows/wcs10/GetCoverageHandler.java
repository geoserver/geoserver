/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wcs10;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geotools.xml.EMFUtils;

public class GetCoverageHandler extends RequestObjectHandler {

    public GetCoverageHandler() {
        super("net.opengis.wcs10.GetCoverageType");
    }

    @Override
    public List<String> getLayers(Object request) {
        String source = (String)EMFUtils.get((EObject)request, "sourceCoverage");
        return source != null ? Arrays.asList(source) : null;
    }

}
