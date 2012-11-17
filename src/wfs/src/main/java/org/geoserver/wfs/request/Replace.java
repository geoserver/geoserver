/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import java.util.List;

import org.eclipse.emf.ecore.EObject;

/**
 * Replace element in a Transaction request.
 *  
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class Replace extends TransactionElement {

    protected Replace(EObject adaptee) {
        super(adaptee);
    }
    
    public abstract List getFeatures();
    
    public static class WFS11 extends Replace {

        public WFS11(EObject adaptee) {
            super(adaptee);
        }
        
        @Override
        public List getFeatures() {
            return eGet(adaptee, "feature", List.class);
        }

    }
    
    public static class WFS20 extends Replace {

        public WFS20(EObject adaptee) {
            super(adaptee);
        }
        
        @Override
        public List getFeatures() {
            return eGet(adaptee, "any", List.class);
        }

    }

}
