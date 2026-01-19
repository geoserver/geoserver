/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import net.opengis.wfs.NativeType;
import net.opengis.wfs.WfsFactory;
import org.eclipse.emf.ecore.EObject;

/**
 * Native element in a Transaction request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class Native extends TransactionElement {

    protected Native(EObject adaptee) {
        super(adaptee);
    }

    public boolean isSafeToIgnore() {
        return eGet(adaptee, "safeToIgnore", Boolean.class);
    }

    public String getVendorId() {
        return eGet(adaptee, "vendorId", String.class);
    }

    public static class WFS11 extends Native {
        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        public static NativeType unadapt(Native nativ) {
            NativeType n = WfsFactory.eINSTANCE.createNativeType();
            n.setSafeToIgnore(nativ.isSafeToIgnore());
            n.setVendorId(nativ.getVendorId());
            // TODO: value
            return n;
        }
    }

    public static class WFS20 extends Native {
        public WFS20(EObject adaptee) {
            super(adaptee);
        }
    }
}
