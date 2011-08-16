/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wcs10;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geotools.xml.EMFUtils;

public class DescribeCoverageHandler extends RequestObjectHandler {

    public DescribeCoverageHandler() {
        super("net.opengis.wcs10.DescribeCoverageType");
    }

    @Override
    public List<String> getLayers(Object request) {
        List l = (List)EMFUtils.get((EObject)request, "coverage");
        return l != null ? new ArrayList(l) : null; 
    }

}
