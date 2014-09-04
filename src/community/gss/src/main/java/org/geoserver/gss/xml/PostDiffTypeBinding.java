/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import javax.xml.namespace.QName;

import net.opengis.wfs.TransactionType;

import org.geoserver.gss.PostDiffType;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

public class PostDiffTypeBinding extends GSSRequestBinding {

    public QName getTarget() {
        return GSS.PostDiffType;
    }

    public Class getType() {
        return PostDiffType.class;
    }

    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        PostDiffType pd = new PostDiffType();
        if (node.hasAttribute("fromVersion")) {
            pd.setFromVersion(((Number) node.getAttributeValue("fromVersion")).longValue());
        }
        if (node.hasAttribute("toVersion")) {
            pd.setToVersion(((Number) node.getAttributeValue("toVersion")).longValue());
        }
        if (node.hasAttribute("typeName")) {
            pd.setTypeName((QName) node.getAttributeValue("typeName"));
        }
        if (node.hasChild("Changes")) {
            pd.setTransaction((TransactionType) node.getChildValue("Changes"));
        }
        setServiceVersion(node, pd);

        return pd;
    }

    @Override
    public Object getProperty(Object object, QName name) throws Exception {
        PostDiffType pd = (PostDiffType) object;

        if (name.getLocalPart().equals("typeName")) {
            return pd.getTypeName();
        } else if (name.getLocalPart().equals("fromVersion")) {
            return pd.getFromVersion();
        } else if (name.getLocalPart().equals("toVersion")) {
            return pd.getToVersion();
        } else if (name.getLocalPart().equals("Changes")) {
            return pd.getTransaction();
        }

        return super.getProperty(object, name);
    }

}
