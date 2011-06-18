package org.geoserver.monitor.ows.wfs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.geotools.xml.EMFUtils;

public class DescribeFeatureTypeHandler extends WFSRequestObjectHandler {

    public DescribeFeatureTypeHandler() {
        super("net.opengis.wfs.DescribeFeatureTypeType");
    }

    @Override
    public List<String> getLayers(Object request) {
        List typeNames = (List) EMFUtils.get((EObject)request, "typeName");
        List<String> layers = new ArrayList<String>();
        for (Object o : typeNames) {
            layers.add(toString(o));
        }
        return layers;
    }

}
