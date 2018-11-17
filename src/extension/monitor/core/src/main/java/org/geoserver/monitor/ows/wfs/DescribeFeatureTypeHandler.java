/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wfs;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.monitor.MonitorConfig;
import org.geotools.xsd.EMFUtils;

public class DescribeFeatureTypeHandler extends WFSRequestObjectHandler {

    public DescribeFeatureTypeHandler(MonitorConfig config, Catalog catalog) {
        super("net.opengis.wfs.DescribeFeatureTypeType", config, catalog);
    }

    @Override
    public List<String> getLayers(Object request) {
        @SuppressWarnings("unchecked")
        List<Object> typeNames = (List<Object>) EMFUtils.get((EObject) request, "typeName");
        if (typeNames == null) {
            return null;
        }

        List<String> layers = new ArrayList<String>();
        for (Object o : typeNames) {
            layers.add(toString(o));
        }
        return layers;
    }

    @Override
    protected List<Object> getElements(Object request) {
        // TODO Auto-generated method stub
        return null;
    }
}
