/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wfs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geotools.xml.EMFUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class DescribeFeatureTypeHandler extends WFSRequestObjectHandler {

    public DescribeFeatureTypeHandler(CoordinateReferenceSystem logCrs, Catalog catalog) {
        super("net.opengis.wfs.DescribeFeatureTypeType", logCrs, catalog);
    }

    @Override
    public List<String> getLayers(Object request) {
        List typeNames = (List) EMFUtils.get((EObject)request, "typeName");
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
