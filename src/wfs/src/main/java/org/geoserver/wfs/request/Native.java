package org.geoserver.wfs.request;

import net.opengis.wfs.NativeType;
import net.opengis.wfs.WfsFactory;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.wfs.request.Insert.WFS11;

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
            //TODO: value
            return n;
        }
    }
    
    public static class WFS20 extends Native {
        public WFS20(EObject adaptee) {
            super(adaptee);
        }
    }

}
