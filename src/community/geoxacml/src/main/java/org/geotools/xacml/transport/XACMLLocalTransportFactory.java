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
