/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import java.util.List;

import javax.xml.namespace.QName;

import org.geoserver.gss.GetCentralRevisionType;
import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

public class GetCentralRevisionTypeBinding extends GSSRequestBinding {

    public QName getTarget() {
        return GSS.GetCentralRevisionType;
    }

    public Class getType() {
        return GetCentralRevisionType.class;
    }
    
    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        List<QName> typeNames = node.getChildValues(QName.class);
        GetCentralRevisionType result = new GetCentralRevisionType();
        result.getTypeNames().addAll(typeNames);
        setServiceVersion(node, result);
        
        return result;
    }
    
    @Override
    public Object getProperty(Object object, QName name) throws Exception {
        if(name.getLocalPart().equals("TypeName")) {
            return ((GetCentralRevisionType) object).getTypeNames();
        }
        return super.getProperty(object, name);
    }

}
