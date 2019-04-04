/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

/**
 * Process parameter input / output for objects which are represented as binary streams (base64
 * encoding required when embeeding them in the XML)
 *
 * @author Andrea Aime, OpenGEO
 */
public abstract class BinaryPPIO extends ComplexPPIO {

    public static final String QUALITY_KEY = "quality";

    protected BinaryPPIO(Class externalType, Class internalType, String mimeType) {
        super(externalType, internalType, mimeType);
    }
}
