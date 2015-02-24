/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.xacml.transport;

import com.sun.xacml.PDP;

/**
 * Default factory creating transport objects for a local PDP
 * 
 * @author Mueller Christian
 * 
 */
public class XACMLLocalTransportFactory implements XACMLTransportFactory {

    XACMLTransport transport;

    public XACMLLocalTransportFactory(PDP pdp, boolean multithreaded) {

        transport = new XACMLLocalTransport(pdp, multithreaded);
    }

    public XACMLTransport getXACMLTransport() {
        return transport;
    }

}
