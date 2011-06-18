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
        return Arrays.asList((String)EMFUtils.get((EObject)request, "sourceCoverage"));
    }

}
