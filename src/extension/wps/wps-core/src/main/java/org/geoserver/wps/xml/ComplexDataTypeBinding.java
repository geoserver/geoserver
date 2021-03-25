/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.xml;

import java.util.ArrayList;
import java.util.List;
import net.opengis.wps10.ComplexDataType;
import net.opengis.wps10.Wps10Factory;
import org.eclipse.xsd.XSDElementDeclaration;
import org.geoserver.wps.XMLEncoderDelegate;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ComplexDataTypeBinding extends org.geotools.wps.bindings.ComplexDataTypeBinding {

    public ComplexDataTypeBinding(Wps10Factory factory) {
        super(factory);
    }

    @Override
    public List<Object[]> getProperties(Object object, XSDElementDeclaration element)
            throws Exception {
        ComplexDataType complex = (ComplexDataType) object;
        if (!complex.getData().isEmpty()
                && complex.getData().get(0) instanceof XMLEncoderDelegate) {
            XMLEncoderDelegate delegate = (XMLEncoderDelegate) complex.getData().get(0);
            List<Object[]> properties = new ArrayList<>();
            properties.add(new Object[] {delegate.getProcessParameterIO().getElement(), delegate});

            return properties;
        }

        return super.getProperties(object, element);
    }

    @Override
    @SuppressWarnings("unchecked") // EMF model without generics
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        ComplexDataType cd = (ComplexDataType) super.parse(instance, node, value);

        // handle non xml content as well
        if (cd.getData().size() == 0) {
            cd.getData().add(instance.getText().toString());
        }

        return cd;
    }

    /** Override to handle plain text contents */
    @Override
    public Element encode(Object object, Document document, Element value) throws Exception {
        ComplexDataType complex = (ComplexDataType) object;
        if (!complex.getData().isEmpty() && complex.getData().get(0) instanceof String) {
            Object v = complex.getData().get(0);
            if (v != null) {
                value.appendChild(document.createTextNode(v.toString()));
            }
        }
        return value;
    }
}
