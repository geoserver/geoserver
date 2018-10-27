/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wcs11;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geotools.xsd.EMFUtils;

public class DescribeCoverageHandler extends RequestObjectHandler {

    public DescribeCoverageHandler(MonitorConfig config) {
        super("net.opengis.wcs11.DescribeCoverageType", config);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getLayers(Object request) {
        return new ArrayList<String>((List<String>) EMFUtils.get((EObject) request, "identifier"));
    }
}
