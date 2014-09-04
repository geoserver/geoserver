/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import javax.xml.namespace.QName;

import org.geoserver.gss.GetDiffType;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

public class GetDiffTypeBinding extends GSSRequestBinding {

    public QName getTarget() {
        return GSS.GetDiff;
    }

    public Class getType() {
        return GetDiffType.class;
    }

    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        GetDiffType gd = new GetDiffType();
        if (node.hasAttribute("fromVersion")) {
            gd.setFromVersion(((Number) node.getAttributeValue("fromVersion")).longValue());
        }
        if (node.hasAttribute("typeName")) {
            gd.setTypeName((QName) node.getAttributeValue("typeName"));
        }
        setServiceVersion(node, gd);

        return gd;
    }

    @Override
    public Object getProperty(Object object, QName name) throws Exception {
        GetDiffType pd = (GetDiffType) object;

        if (name.getLocalPart().equals("typeName")) {
            return pd.getTypeName();
        } else if (name.getLocalPart().equals("fromVersion")) {
            return pd.getFromVersion();
        }

        return super.getProperty(object, name);
    }

}
