/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Process parameter input / output for objects which are text based (no Base64 encoding needed)
 *  
 * @author Andrea Aime, OpenGEO
 */
public abstract class CDataPPIO extends ComplexPPIO {

	protected CDataPPIO(Class externalType, Class internalType, String mimeType) {
		super(externalType, internalType, mimeType);
	}
	
	/**
     * Encodes the internal object representation to the provided writer
     */
    public abstract void encode( Object value, OutputStream os) throws IOException;
    
    @Override
    public Object decode(Object input) throws Exception {
    	return decode((String) input);
    }

    /**
     * Decodes a String into the internal object (used for CDATA inputs) 
     * @param input
     * @return
     * @throws Exception
     */
    public abstract Object decode(String input) throws Exception;
}
