/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.net.URL;

/**
 * Parses interpolation values
 * @author Andrea Aime - GeoSolutions
 *
 */
public class URLPPIO extends LiteralPPIO {

    protected URLPPIO() {
        super(URL.class);
    }
    
    @Override
    public Object decode(String value) throws Exception {
        return new URL(value);
    }

    @Override
    public String encode(Object value) throws Exception {
        return ((URL) value).toExternalForm();
    }
}
