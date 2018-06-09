/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import javax.xml.namespace.QName;
import net.opengis.wfs.PropertyType;
import net.opengis.wfs.WfsFactory;
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

    public abstract QName getName();

    public static class WFS11 extends Property {
        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public QName getName() {
            return eGet(adaptee, "name", QName.class);
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
    }
}
