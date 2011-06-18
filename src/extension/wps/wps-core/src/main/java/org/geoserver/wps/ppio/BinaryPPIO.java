/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.OutputStream;

/**
 * Process parameter input / output for objects which are represented as binary streams (base64 encoding required when
 * embeeding them in the XML)
 *  
 * @author Andrea Aime, OpenGEO
 */
public abstract class BinaryPPIO extends ComplexPPIO {

	protected BinaryPPIO(Class externalType, Class internalType, String mimeType) {
		super(externalType, internalType, mimeType);
	}
	
	/**
     * Encodes the internal object representation of a parameter as a string.
     */
    public abstract void encode( Object value, OutputStream os) throws Exception;

}
