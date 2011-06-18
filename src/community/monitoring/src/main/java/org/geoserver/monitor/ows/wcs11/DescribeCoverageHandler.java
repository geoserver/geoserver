package org.geoserver.monitor.ows.wcs11;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geotools.xml.EMFUtils;

public class DescribeCoverageHandler extends RequestObjectHandler {

    public DescribeCoverageHandler() {
        super("net.opengis.wcs11.DescribeCoverageType");
    }

    @Override
    public List<String> getLayers(Object request) {
        return new ArrayList((List)EMFUtils.get((EObject)request, "identifier"));
    }

}
