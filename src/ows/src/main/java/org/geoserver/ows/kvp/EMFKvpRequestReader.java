/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.ServiceException;
import org.geotools.xsd.EMFUtils;

/**
 * Web Feature Service Key Value Pair Request reader.
 *
 * <p>This request reader makes use of the Eclipse Modelling Framework reflection api.
 *
 * @author Justin Deoliveira, The Open Planning Project
 * @author Andrea Aime, TOPP
 */
public class EMFKvpRequestReader extends KvpRequestReader {
    /** Factory used to create model objects / requests. */
    protected EFactory factory;

    /**
     * Creates the Wfs Kvp Request reader.
     *
     * @param requestBean The request class, which must be an emf class.
     */
    public EMFKvpRequestReader(Class requestBean, EFactory factory) {
        super(requestBean);

        // make sure an eobject is passed in
        if (!EObject.class.isAssignableFrom(requestBean)) {
            String msg = "Request bean must be an EObject";
            throw new IllegalArgumentException(msg);
        }

        this.factory = factory;
    }

    /** Reflectivley creates the request bean instance. */
    public Object createRequest() {
        String className = getRequestBean().getName();

        // strip off package
        int index = className.lastIndexOf('.');

        if (index != -1) {
            className = className.substring(index + 1);
        }

        Method create = OwsUtils.method(factory.getClass(), "create" + className);

        try {
            return create.invoke(factory, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        // use emf reflection
        EObject eObject = (EObject) request;

        for (Iterator e = kvp.entrySet().iterator(); e.hasNext(); ) {
            Map.Entry entry = (Map.Entry) e.next();
            String property = (String) entry.getKey();
            Object value = entry.getValue();

            // respect the filter
            if (filter(property)) {
                continue;
            }

            if (EMFUtils.has(eObject, property)) {
                try {
                    setValue(eObject, property, value);
                } catch (Exception ex) {
                    throw new ServiceException(
                            "Failed to set property "
                                    + property
                                    + " in request object using value "
                                    + value
                                    + (value != null ? " of type " + value.getClass() : ""),
                            ex,
                            ServiceException.INVALID_PARAMETER_VALUE,
                            property);
                }
            }
        }

        return request;
    }

    /**
     * Sets a value in the target EMF object, adding it to a collection if the target is a
     * collection, setting it otherwise. Subclasses can override this behavior
     */
    protected void setValue(EObject eObject, String property, Object value) {
        // check for a collection
        if (EMFUtils.isCollection(eObject, property)) {
            EMFUtils.add(eObject, property, value);
        } else {
            EMFUtils.set(eObject, property, value);
        }
    }
}
