/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import javax.xml.namespace.QName;

import org.geoserver.gss.PostDiffResponseType;
import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

public class PostDiffResponseTypeBinding extends AbstractComplexBinding {

    public QName getTarget() {
        return GSS.PostDiffResponseType;
    }

    public Class getType() {
        return PostDiffResponseType.class;
    }
    
    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        return new PostDiffResponseType();
    }
    
    @Override
    public Object getProperty(Object object, QName name) throws Exception {
        if("success".equals(name.getLocalPart())) {
            return true;
        }
        return super.getProperty(object, name);
    }

}
