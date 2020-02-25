/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import javax.xml.transform.TransformerException;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.xml.transform.TransformerBase;
import org.springframework.util.Assert;

/**
 * An OWS {@link Response} handler that knows how to encode a {@link XMLTransformerMap}
 *
 * @author Gabriel Roldan
 * @see XMLTransformerMap
 */
public class XMLTransformerMapResponse extends AbstractMapResponse {

    public XMLTransformerMapResponse() {
        super(XMLTransformerMap.class, (Set<String>) null);
    }

    /**
     * Encodes the {@link XMLTransformerMap} down to the given destination output stream.
     *
     * @param value an {@link XMLTransformerMap}
     * @param output xml stream destination
     * @param operation operation descriptor for which the map was produced; not used at all.
     */
    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        write(value, output);
    }

    public void write(Object value, OutputStream output) throws IOException, ServiceException {
        Assert.isInstanceOf(XMLTransformerMap.class, value);

        XMLTransformerMap map = (XMLTransformerMap) value;
        TransformerBase transformer = map.getTransformer();
        Object transformerSubject = map.getTransformerSubject();
        try {
            transformer.transform(transformerSubject, output);
        } catch (TransformerException e) {
            // TransformerException do not respect the Exception.getCause() contract
            Throwable cause = e.getCause() != null ? e.getCause() : e.getException();
            // we need to propagate the RuntimeException
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new ServiceException("getmap operation failed.", cause != null ? cause : e);
        } finally {
            map.dispose();
        }
    }
}
