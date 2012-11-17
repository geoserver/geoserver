/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfsv;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.wfs.request.TransactionElement;

public class Rollback extends TransactionElement {

    protected Rollback(EObject adaptee) {
        super(adaptee);
    }

    
    public String getToFeatureVersion() {
        return eGet(adaptee, "toFeatureVersion", String.class);
    }

    public String getUser() {
        return eGet(adaptee, "user", String.class);
    }
    
    public static class WFS11 extends Rollback {

        public WFS11(EObject adaptee) {
            super(adaptee);
        }

    }

}
