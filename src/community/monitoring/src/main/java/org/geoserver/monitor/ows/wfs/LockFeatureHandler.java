/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wfs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.xml.EMFUtils;
import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;

public class LockFeatureHandler extends WFSRequestObjectHandler {

    public LockFeatureHandler() {
        super("net.opengis.wfs.LockFeatureType");
    }

    @Override
    public List<String> getLayers(Object request) {
        List locks = (List) EMFUtils.get((EObject)request, "lock");
        if (locks == null) {
            return null;
        }
        
        List<String> layers = new ArrayList();
        for (Object lock : locks) {
            layers.add(toString(EMFUtils.get((EObject)lock, "typeName")));
        }
        
        return layers;
    }
    
    @Override
    protected BoundingBox getBBox(Object request) {
        List locks = (List) OwsUtils.get(request, "lock");
        BBoxFilterVisitor visitor = new BBoxFilterVisitor();
        for(Object l : locks){
            Filter f = (Filter) OwsUtils.get(l, "filter");
            if(f!=null) f.accept(visitor, null);
        }
        return visitor.getBbox();
    }
}
