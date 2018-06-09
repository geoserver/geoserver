/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * A generic service object invocation logger based on EMF reflection
 *
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

        for (Iterator p = properties.iterator(); p.hasNext(); ) {
            EStructuralFeature property = (EStructuralFeature) p.next();
            Object value = object.eGet(property);

            // skip empty properties
            if (value == null
                    || (value instanceof Collection && ((Collection) value).isEmpty())
                    || (value instanceof Map && ((Map) value).isEmpty())) {
                continue;
            }

            log.append("\n");

            for (int i = 0; i < level; i++) log.append("    ");

            if (value instanceof EObject && (level < 3)) {
                log.append(property.getName());
                log.append(":");
                log((EObject) value, level + 1, log);
            } else if (value instanceof Collection) {
                log(property.getName(), (Collection) value, level + 1, log);
            } else {
                log.append(property.getName());
                log.append(" = " + value);
            }
        }
    }

    protected void log(String property, Collection collection, int level, StringBuffer log) {
        int count = 0;
        for (Object o : collection) {
            String pc = property + "[" + count + "]";
            if (o instanceof EObject) {
                log.append(pc);
                log.append(":");
                log((EObject) o, level, log);
            } else if (o instanceof Collection) {
                log(pc, (Collection) o, level + 1, log);
            } else {
                log.append(pc).append(" = " + o);
            }
        }
    }
}
