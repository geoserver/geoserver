/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import javax.xml.namespace.QName;

import org.geoserver.gss.CentralRevisionsType;
import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

public class LayerRevisionTypeBinding extends AbstractComplexBinding {

    public QName getTarget() {
        return GSS.LayerRevisionType;
    }

    public Class getType() {
        return CentralRevisionsType.LayerRevision.class;
    }
    
    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        QName typeName = (QName) node.getAttributeValue("typeName");
        long revision = ((Number) node.getAttributeValue("centralRevision")).longValue();
        return new CentralRevisionsType.LayerRevision(typeName, revision);
    }
    
    @Override
    public Object getProperty(Object object, QName name) throws Exception {
        CentralRevisionsType.LayerRevision lr = (CentralRevisionsType.LayerRevision) object;
        if(name.getLocalPart().equals("typeName")) {
            return lr.getTypeName();
        } else if(name.getLocalPart().equals("centralRevision")) {
            return lr.getCentralRevision();
        } else { 
            return super.getProperty(object, name);
        }
    }

}
