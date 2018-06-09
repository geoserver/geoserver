/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.WfsFactory;
import org.eclipse.emf.ecore.EObject;

/**
 * Delete element in a Transaction request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class Delete extends TransactionElement {

    protected Delete(EObject adaptee) {
        super(adaptee);
    }

    public static class WFS11 extends Delete {
        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        public static DeleteElementType unadapt(Delete delete) {
            DeleteElementType de = WfsFactory.eINSTANCE.createDeleteElementType();
            de.setHandle(delete.getHandle());
            de.setTypeName(delete.getTypeName());
            de.setFilter(delete.getFilter());
            return de;
        }
    }

    public static class WFS20 extends Delete {
        public WFS20(EObject adaptee) {
            super(adaptee);
        }
    }
}
