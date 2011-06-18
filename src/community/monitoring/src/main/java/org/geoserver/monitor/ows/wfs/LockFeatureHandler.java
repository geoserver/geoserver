/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wfs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.geotools.xml.EMFUtils;

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

}
