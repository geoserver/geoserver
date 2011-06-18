package org.geoserver.monitor.ows.wcs11;

import java.util.Arrays;
import java.util.List;

import net.opengis.ows11.CodeType;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geotools.xml.EMFUtils;

public class GetCoverageHandler extends RequestObjectHandler {

    public GetCoverageHandler() {
        super("net.opengis.wcs11.GetCoverageType");
    }
    
    @Override
    public List<String> getLayers(Object request) {
        return Arrays.asList(
            ((CodeType)EMFUtils.get((EObject)request, "identifier")).getValue());
    }

}
