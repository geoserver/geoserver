/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfsv.kvp;

import net.opengis.wfsv.WfsvFactory;

import org.geoserver.ows.kvp.EMFKvpRequestReader;


/**
 * Web Feature Service Key Value Pair Request reader.
 * <p>
 * This request reader makes use of the Eclipse Modelling Framework
 * reflection api.
 * </p>
 * @author Andrea Aime, TOPP
 *
 */
public class WFSVKvpRequestReader extends EMFKvpRequestReader {

    /**
     * Creates the Wfs Kvp Request reader.
     *
     * @param requestBean The request class, which must be an emf class.
     */
    public WFSVKvpRequestReader(Class requestBean) {
        super(requestBean, WfsvFactory.eINSTANCE);
    }
    
    WfsvFactory getWfsFactory() {
        return (WfsvFactory) factory;
    }
}
