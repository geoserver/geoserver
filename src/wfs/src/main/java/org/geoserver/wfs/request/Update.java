/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import java.util.ArrayList;
import java.util.List;
import net.opengis.wfs.UpdateElementType;
import net.opengis.wfs.WfsFactory;
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
    }
}
