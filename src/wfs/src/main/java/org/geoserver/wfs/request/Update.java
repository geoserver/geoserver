/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import java.util.ArrayList;
import java.util.List;
import net.opengis.wfs.PropertyType;
import net.opengis.wfs.UpdateElementType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.UpdateType;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.wfs.request.Insert.WFS11;

/**
 * Update element in a Transaction request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class Update extends TransactionElement {

    protected Update(EObject adaptee) {
        super(adaptee);
    }

    public abstract List<Property> getUpdateProperties();

    public abstract void setUpdateProperties(List<Property> properties);

    public abstract Property createProperty();

    public static class WFS11 extends Update {
        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public List<Property> getUpdateProperties() {
            List<Property> list = new ArrayList();
            for (Object o : eGet(adaptee, "property", List.class)) {
                list.add(new Property.WFS11((EObject) o));
            }
            return list;
        }

        @Override
        public void setUpdateProperties(List<Property> properties) {
            UpdateElementType update = (UpdateElementType) adaptee;
            update.getProperty().clear();
            properties.stream().map(p -> p.getAdaptee()).forEach(p -> update.getProperty().add(p));
        }

        @Override
        public Property createProperty() {
            PropertyType property = WfsFactory.eINSTANCE.createPropertyType();
            return new Property.WFS11(property);
        }

        public static UpdateElementType unadapt(Update update) {
            if (update instanceof WFS11) {
                return (UpdateElementType) update.getAdaptee();
            }

            UpdateElementType ue = WfsFactory.eINSTANCE.createUpdateElementType();
            ue.setHandle(update.getHandle());
            ue.setTypeName(update.getTypeName());
            ue.setFilter(update.getFilter());

            for (Property p : update.getUpdateProperties()) {
                ue.getProperty().add(Property.WFS11.unadapt(p));
            }
            return ue;
        }
    }

    public static class WFS20 extends Update {
        public WFS20(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public List<Property> getUpdateProperties() {
            List<Property> list = new ArrayList();
            for (Object o : eGet(adaptee, "property", List.class)) {
                list.add(new Property.WFS20((EObject) o));
            }
            return list;
        }

        @Override
        public void setUpdateProperties(List<Property> properties) {
            UpdateType update = (UpdateType) adaptee;
            update.getProperty().clear();
            properties
                    .stream()
                    .map(p -> (net.opengis.wfs20.PropertyType) p.getAdaptee())
                    .forEach(p -> update.getProperty().add(p));
        }

        @Override
        public Property createProperty() {
            net.opengis.wfs20.PropertyType property = Wfs20Factory.eINSTANCE.createPropertyType();
            return new Property.WFS20(property);
        }
    }
}
