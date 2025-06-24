/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import org.eclipse.emf.ecore.EObject;

/**
 * WFS DescribeFeatureType request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class DescribeFeatureTypeRequest extends RequestObject {

    public static DescribeFeatureTypeRequest adapt(Object request) {
        if (request instanceof EObject type1) {
            return new WFS11(type1);
        } else if (request instanceof EObject type) {
            return new WFS20(type);
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
