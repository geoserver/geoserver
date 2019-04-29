/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Encoder;

/**
 * A response designed to encode a specific object into XML
 *
 * @author Andrea Aime - TOPP
 */
public class XmlObjectEncodingResponse extends Response {

    protected String elementName;
    protected Class<?> xmlConfiguration;

    public XmlObjectEncodingResponse(
            Class<?> binding, String elementName, Class<?> xmlConfiguration) {
        super(binding);
        this.elementName = elementName;
        this.xmlConfiguration = xmlConfiguration;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "application/xml";
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        try {
            Configuration c =
                    (Configuration) xmlConfiguration.getDeclaredConstructor().newInstance();
            Encoder e = new Encoder(c);
            for (Map.Entry<String, String> entry : getSchemaLocations().entrySet()) {
                e.setSchemaLocation(entry.getKey(), entry.getValue());
            }
            configureEncoder(e, elementName, xmlConfiguration);

            e.encode(value, new QName(c.getXSD().getNamespaceURI(), elementName), output);
        } catch (Exception e) {
            throw (IOException) new IOException().initCause(e);
        }
    }

    /**
     * Allows subclasses to further configure the encoder
     *
     * @param encoder encoder used for output
     * @param elementName Element being configured
     * @param xmlConfiguration Configuration
     */
    protected void configureEncoder(
            Encoder encoder, String elementName, Class<?> xmlConfiguration) {
        // nothing to do here, subclasses will do their own magic
    }

    /** Subclasses can override this method to return the necessary schema location declarations */
    protected Map<String, String> getSchemaLocations() {
        return Collections.emptyMap();
    }
}
