/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import javax.xml.namespace.QName;

import net.opengis.wfs.TransactionType;

import org.geoserver.gss.GetDiffResponseType;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

public class GetDiffResponseTypeBinding extends GSSRequestBinding {

    public QName getTarget() {
        return GSS.GetDiffResponse;
    }

    public Class getType() {
        return GetDiffResponseType.class;
    }

    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        GetDiffResponseType gdr = new GetDiffResponseType();
        if (node.hasAttribute("fromVersion")) {
            gdr.setFromVersion(((Number) node.getAttributeValue("fromVersion")).longValue());
        }
        if (node.hasAttribute("toVersion")) {
            gdr.setToVersion(((Number) node.getAttributeValue("toVersion")).longValue());
        }
        if (node.hasAttribute("typeName")) {
            gdr.setTypeName((QName) node.getAttributeValue("typeName"));
        }
        if (node.hasChild("Changes")) {
            gdr.setTransaction((TransactionType) node.getChildValue("Changes"));
        }

        return gdr;
    }

    @Override
    public Object getProperty(Object object, QName name) throws Exception {
        GetDiffResponseType gdr = (GetDiffResponseType) object;

        if (name.getLocalPart().equals("typeName")) {
            return gdr.getTypeName();
        } else if (name.getLocalPart().equals("fromVersion")) {
            return gdr.getFromVersion();
        } else if (name.getLocalPart().equals("toVersion")) {
            return gdr.getToVersion();
        } else if (name.getLocalPart().equals("Changes")) {
            return gdr.getTransaction();
        }

        return super.getProperty(object, name);
    }

}
