/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wfs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.xml.EMFUtils;
import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;

public class GetFeatureHandler extends WFSRequestObjectHandler {

    public GetFeatureHandler(Catalog catalog) {
        super("net.opengis.wfs.GetFeatureType", catalog);
    }

    @Override
    public List<String> getLayers(Object request) {
        List queries = (List) EMFUtils.get((EObject)request,"query");
        if (queries == null) {
            return null;
        }
        
        List<String> layers = new ArrayList();
        for (Object q : queries) {
            List typeNames = (List) EMFUtils.get((EObject) q, "typeName");
            
            for (Object o : typeNames) {
                layers.add(toString(o));
            }
             
        }
        return layers;
    }

    @Override
    protected BoundingBox getBBox(Object request) {
        List queries = (List) OwsUtils.get(request, "query");
        BBoxFilterVisitor visitor = new BBoxFilterVisitor();
        for(Object q : queries){
            Filter f = (Filter) OwsUtils.get(q, "filter");
            if(f!=null) f.accept(visitor, null);
        }
        return visitor.getBbox();
    }

}
