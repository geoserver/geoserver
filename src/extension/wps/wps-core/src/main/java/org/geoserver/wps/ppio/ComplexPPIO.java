/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Process parameter input / output for arbitrary data on a specific mime type.
 *
 * @author Lucas Reed, Refractions Research Inc
 * @author Justin Deoliveira, OpenGEO
 */
public abstract class ComplexPPIO extends ProcessParameterIO {

    /** mime type of encoded content. */
    protected String mimeType;

    /** Constructor. */
    protected ComplexPPIO(Class externalType, Class internalType, String mimeType) {
        super(externalType, internalType);
        this.mimeType = mimeType;
    }

    /** The mime type of the parameter of the data in encoded form. */
    public final String getMimeType() {
        return mimeType;
    }

    /**
     * Decodes the parameter from an external source or input stream.
     *
     * <p>This method should parse the input stream into its "internal" representation.
     *
     * @param input The input stream.
     * @return An object of type {@link #getType()}.
     */
    public abstract Object decode(InputStream input) throws Exception;

    /**
     * Decodes the parameter from an extenral source that has been pre-parsed.
     *
     * <p>This method should transform the object from the external representation to the internal
     * representation.
     *
     * @param input An object of type {@link #getExternalType()}
     * @return An object of type {@link #getType()}.
     */
    public Object decode(Object input) throws Exception {
        return input;
    }

    /** Encodes the internal object representation of a parameter into an output stream */
    public abstract void encode(Object value, OutputStream os) throws Exception;

    /**
     * Encodes the internal object representation of a parameter into an output stream using
     * specific encoding parameters
     */
    public void encode(Object value, Map<String, Object> encodingParameters, OutputStream os)
            throws Exception {
        encode(value, os);
    };

    /**
     * Provides a suitable extension for the output file. Implement this if the file extension is
     * not depend on the object being encoded
     */
    public String getFileExtension() {
        return ".bin";
    }

    /**
     * Provides a suitable extension for the output file given the object being encoded. The default
     * implementation simply calls {@link #getFileExtension()}
     */
    public String getFileExtension(Object object) {
        return getFileExtension();
    }
}
