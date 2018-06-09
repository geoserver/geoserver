/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import net.opengis.wfs.DescribeFeatureTypeType;
import org.eclipse.emf.ecore.EObject;

/**
 * WFS DescribeFeatureType request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class DescribeFeatureTypeRequest extends RequestObject {

    public static DescribeFeatureTypeRequest adapt(Object request) {
        if (request instanceof DescribeFeatureTypeType) {
            return new WFS11((EObject) request);
        } else if (request instanceof net.opengis.wfs20.DescribeFeatureTypeType) {
            return new WFS20((EObject) request);
        }
        return null;
    }

    protected DescribeFeatureTypeRequest(EObject adaptee) {
        super(adaptee);
    }

    public static class WFS11 extends DescribeFeatureTypeRequest {

        public WFS11(EObject adaptee) {
            super(adaptee);
        }
    }

    public static class WFS20 extends DescribeFeatureTypeRequest {

        public WFS20(EObject adaptee) {
            super(adaptee);
        }
    }
}
