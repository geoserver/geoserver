/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import javax.xml.namespace.QName;

import org.geoserver.gss.GSSRequest;
import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.Node;

/**
 * Base class that deals with service and version attributes
 * 
 * @author Andrea Aime - OpenGeo
 */
public abstract class GSSRequestBinding extends AbstractComplexBinding {

    protected void setServiceVersion(Node node, GSSRequest request) {
        if (node.getAttributeValue("service") != null) {
            request.setService((String) node.getAttributeValue("service"));
        } else {
            request.setService("GSS");
        }

        if (node.getAttributeValue("version") != null) {
            request.setVersion((String) node.getAttributeValue("version"));
        } else {
            request.setVersion("1.0.0");
        }
    }

    @Override
    public Object getProperty(Object object, QName name) throws Exception {
        if (name.getLocalPart().equals("service")) {
            return "GSS";
        } else if (name.getLocalPart().equals("version")) {
            return "1.0.0";
        }

        return super.getProperty(object, name);
    }

}
