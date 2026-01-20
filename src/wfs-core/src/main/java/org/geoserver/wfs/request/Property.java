/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import javax.xml.namespace.QName;
import net.opengis.wfs.PropertyType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.ValueReferenceType;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.ecore.EObject;

/**
 * Property of an Update element in a Transaction.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class Property extends RequestObject {

    protected Property(EObject adaptee) {
        super(adaptee);
    }

    public Object getValue() {
        return eGet(adaptee, "value", Object.class);
    }

    public void setValue(Object value) {
        eSet(adaptee, "value", value);
    }

    public abstract QName getName();

    public abstract void setName(QName name);

    public static class WFS11 extends Property {
        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public QName getName() {
            return eGet(adaptee, "name", QName.class);
        }

        @Override
        public void setName(QName name) {
            eSet(adaptee, "name", name);
        }

        public static PropertyType unadapt(Property property) {
            PropertyType p = WfsFactory.eINSTANCE.createPropertyType();
            p.setName(property.getName());
            p.setValue(property.getValue());
            return p;
        }
    }

    public static class WFS20 extends Property {
        public WFS20(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public QName getName() {
            return eGet(adaptee, "valueReference.value", QName.class);
        }

        @Override
        public void setName(QName name) {
            net.opengis.wfs20.PropertyType property = (net.opengis.wfs20.PropertyType) adaptee;
            ValueReferenceType valueReference = Wfs20Factory.eINSTANCE.createValueReferenceType();
            valueReference.setValue(name);
            property.setValueReference(valueReference);
        }
    }
}
