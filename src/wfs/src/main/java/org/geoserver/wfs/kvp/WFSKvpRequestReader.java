/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import net.opengis.wfs.WfsFactory;
import org.eclipse.emf.ecore.EFactory;
import org.geoserver.ows.kvp.EMFKvpRequestReader;

/**
 * Web Feature Service Key Value Pair Request reader.
 *
 * <p>This request reader makes use of the Eclipse Modelling Framework reflection api.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class WFSKvpRequestReader extends EMFKvpRequestReader {

    /**
     * Creates the Wfs Kvp Request reader.
     *
     * @param requestBean The request class, which must be an emf class.
     */
    public WFSKvpRequestReader(Class requestBean) {
        this(requestBean, WfsFactory.eINSTANCE);
    }

    /**
     * Creates the Wfs Kvp Request reader specifying the factory.
     *
     * @param requestBean The request class, which must be an emf class.
     * @param factory The emf factory for the request bean.
     */
    public WFSKvpRequestReader(Class requestBean, EFactory factory) {
        super(requestBean, factory);
    }

    protected EFactory getFactory() {
        return factory;
    }
}
