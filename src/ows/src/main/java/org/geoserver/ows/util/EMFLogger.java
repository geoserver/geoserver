/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows.util;

import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * A generic service object invocation logger based on EMF reflection
 * @author Justin DeOliveira, TOPP
 */
public class EMFLogger extends RequestObjectLogger {
    
    public EMFLogger(String logPackage) {
        super(logPackage);
    }

    @Override
    protected boolean isRequestObject(Object obj) {
        return obj instanceof EObject;
    }    
    
    protected void log(Object obj, int level, StringBuffer log) {
        EObject object = (EObject) obj;
        List properties = object.eClass().getEAllStructuralFeatures();

        for (Iterator p = properties.iterator(); p.hasNext();) {
            EStructuralFeature property = (EStructuralFeature) p.next();
            Object value = object.eGet(property);

            log.append("\n");

            for (int i = 0; i < level; i++)
                log.append("\t");

            log.append(property.getName());

            if (value instanceof EObject && (level < 2)) {
                log.append(":");
                log((EObject) value, level + 1, log);
            } else {
                log.append(" = " + value);
            }
        }
    }
}
