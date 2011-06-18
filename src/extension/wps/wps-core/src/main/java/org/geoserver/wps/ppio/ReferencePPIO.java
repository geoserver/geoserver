/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.net.URL;

public abstract class ReferencePPIO extends ProcessParameterIO {

    protected ReferencePPIO(Class externalType, Class internalType) {
        super(externalType, internalType);
    }
    
    public abstract URL encode( Object o ); 

}
